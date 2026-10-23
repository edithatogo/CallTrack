#!/usr/bin/env python3
"""Run bounded parity foundations on the dedicated emulator, never a physical phone.

Refuses mismatched builds and pre-existing packages. Store phase changes only its
private fixture database; it grants no call/calendar permissions and makes no calls.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import uuid
from run_slice_build import inputs

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'org.calltrack.community.dev'
TEST_PACKAGE = PACKAGE + '.test'
from parity_targets import AVD, is_dedicated_avd


def qualify(serial, suite="store"):
    if suite not in ("store", "source-identity", "wake-budget"):
        raise ValueError("Unknown native suite")
    if not re.fullmatch(r'emulator-\d+', serial):
        raise RuntimeError('Parity emulator harness refuses a physical-device serial')
    evidence = ROOT / '.local/parity-evidence'
    evidence.mkdir(parents=True, exist_ok=True)
    build = json.loads((ROOT / '.local/slice-evidence/build-receipt.json').read_text())
    if build['exit_code'] != 0 or not build['inputs_unchanged'] or build['inputs'] != inputs():
        raise RuntimeError('Successful build does not match current sources')
    apks = [ROOT / 'app/build/outputs/apk/debug/app-debug.apk',
            ROOT / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']
    for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
        if build['artifacts'].get(str(apk.relative_to(ROOT))) != hashlib.sha256(apk.read_bytes()).hexdigest():
            raise RuntimeError('APK differs from successful build')
        identity = subprocess.check_output([str(ROOT / '.local/android-sdk/build-tools/35.0.0/aapt'),
                                            'dump', 'badging', str(apk)], text=True)
        if "package: name='" + package + "'" not in identity:
            raise RuntimeError('Unexpected APK identity')
    receipt = {'fixture_token': uuid.uuid4().hex, 'phase': suite, 'steps': [],
               'artifacts': build['artifacts'], 'inputs': build['inputs']}
    attempted = []

    def command(*args):
        result = subprocess.run([str(ROOT / '.local/android-sdk/platform-tools/adb'), '-s', serial, *args],
                                capture_output=True, text=True, timeout=180)
        receipt['steps'].append({'args': list(args), 'exit_code': result.returncode,
                                 'output': result.stdout + result.stderr})
        if result.returncode:
            raise RuntimeError('ADB command failed: ' + ' '.join(args))
        return result.stdout.strip()

    if command('shell', 'getprop', 'ro.kernel.qemu') != '1':
        raise RuntimeError('Selected target is not a verified emulator')
    if not is_dedicated_avd(command('emu', 'avd', 'name')):
        raise RuntimeError('Selected emulator is not the dedicated CallTrack AVD')
    if command('shell', 'getprop', 'sys.boot_completed') != '1':
        raise RuntimeError('Emulator has not completed boot')
    for package in (PACKAGE, TEST_PACKAGE):
        if 'package:' + package in command('shell', 'pm', 'list', 'packages', package).splitlines():
            raise RuntimeError('Refusing to replace existing package ' + package)

    def check(class_name, method):
        output = command('shell', 'am', 'instrument', '-w', '-r', '-e', 'fixtureToken', receipt['fixture_token'],
                         '-e', 'class', 'com.markobl.calllogsync.slice.' + class_name + '#' + method,
                         TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner')
        if 'OK (1 test)' not in output or 'FAILURES' in output or 'INSTRUMENTATION_FAILED' in output:
            # Uninstall clears useful process-exit evidence; retain diagnostics first.
            for args in (('shell', 'dumpsys', 'activity', 'exit-info', PACKAGE),
                         ('logcat', '-d', '-b', 'crash', '-v', 'threadtime', '-t', '200')):
                try:
                    command(*args)
                except Exception as diagnostic:
                    receipt.setdefault('diagnostic_errors', []).append(str(diagnostic))
            raise RuntimeError('Native test failed: ' + method)

    try:
        receipt['sdk'] = command('shell', 'getprop', 'ro.build.version.sdk')
        for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
            attempted.append(package)
            if 'Success' not in command('install', str(apk)):
                raise RuntimeError('Install not confirmed')
        if suite == 'wake-budget':
            check('WakeBudgetTest', 'heldDeliveryLockCannotHoldWakePastDeadline')
        elif suite == 'source-identity':
            check('SourceNamespaceStoreTest', 'falseIdentityCommitCannotBeReusedFromCache')
            check('SourceNamespaceStoreTest', 'thrownIdentityCommitCannotBeReusedFromCache')
            check('SourceNamespaceStoreTest', 'committedNamespaceIsReusedAcrossAdapters')
        else:
            check('WorkStatusStoreTest', 'uncertainCommitIsHiddenAcrossStoreInstancesUntilSuccessfulPublication')
            check('AutomaticSettingsTest', 'openingRecoveryDoesNotCreateReceiptsOrEnableLogging')
            check('HistoricalImportStoreTest', 'sessionHistoryPagesNewestFirstWithoutLosingOlderSessions')
            check('AutomaticSettingsTest', 'openingImportsDoesNotCreatePreviewOrConfirmWithoutAccess')
            check('WorkStatusStoreTest', 'publicationPersistsFailureCountAndRejectsOldActivationAfterReopen')
            check('WorkStatusStoreTest', 'legacyStatusWithoutCounterRemainsReadableAndUnknownCodesAreSanitised')
            check('AutomaticSettingsTest', 'openingSettingsDoesNotEnableOrSelectDestinationWithoutAccess')
            check('AutomaticWorkTest', 'absentActivationExecutesWithoutCallCalendarPermission')
            check('AutomaticWorkTest', 'staleGenerationExecutesWithoutOpeningProviders')
            check('AutomaticWorkTest', 'promptAndPeriodicRequestsPersistWithoutNetworkConstraint')
            check('HistoricalImportStoreTest', 'schemaOneMigrationPreservesAggregateProgressWithoutInventingOutcomes')
            check('HistoricalImportStoreTest', 'failedOutcomeProgressCommitRollsBackBothRecords')
            check('HistoricalImportStoreTest', 'frozenPreviewAndPartialDeliverySurviveReopenAndRepeatWithoutDuplicate')
            check('HistoricalImportStoreTest', 'failedCandidateAppendRollsBackPreviewAndStaleRevisionIsRejected')
            check('ActivationStoreTest', 'activationAndPauseCommitToDiskAndRejectOldGeneration')
            check('ActivationStoreTest', 'malformedOrFutureSettingsCannotBecomeEnabled')
            check('ActivationStoreTest', 'falseCommitAfterCacheMutationBlocksAllActivationAdapters')
            check('ActivationStoreTest', 'thrownCommitAfterCacheMutationBlocksAllActivationAdapters')
            check('WorkStatusStoreTest', 'thrownCommitIsHiddenAcrossStoreInstancesUntilSuccessfulPublication')
            check('CalendarSliceTest', 'syntheticCursorMapsPresentationWithoutPersonalQuery')
            check('CalendarSliceTest', 'syntheticCursorRetainsNullAndExtendedPresentation')
            check('CalendarSliceTest', 'syntheticPagingUsesBoundedQueriesAndPreservesEveryRow')
            for method in ('preservesV1ReceiptsAndCreatesLegacyBindings',
                           'outboxCommitsInflightAndPurgesConfirmedPayloadAcrossReopen',
                           'rejectsOutboxWriteInsideAnUncommittedTransaction',
                           'kotlinMappingRecognisesPreservedJavaReceiptWithoutReinsert',
                           'preservesV2PendingRowsWithoutInventingOperationOwnership',
                           'operationIdentitySurvivesReopenAndPayloadPurge',
                           'preservesV3OwnershipWithoutInferringSourceFactsFromPayload',
                           'sourceFactsSurviveReopenAndPayloadPurgeWithNullPresentation',
                           'frozenCheckpointPersistsAndRejectsRegressionOrRebinding',
                           'preservesV4FactsAndCreatesEmptyCheckpointStore',
                           'rolloverPreservesProcessedAnchorAndRejectsStalePublication',
                           'pendingRecoveryQueryIsBoundedAndOwnerDestinationScoped',
                           'deliveryCountsIncludeLateLegacyWithoutDoubleCountingBoundReceipt',
                           'recoveryPagingIncludesLegacyAndRemainsStableWhenStatesChange',
                           'explicitReapprovalPersistsFreshOwnerAndSuppressionClearsReadyPayload',
                           'payloadStorageAgeSurvivesTransitionsAndExpiryPreservesRecoveryMetadata',
                           'preservesV5PendingPayloadAndCheckpointWithExplicitMigrationGrace'):
                check('ParityStoreTest', method)
        receipt['result'] = 'passed'
    except Exception as error:
        receipt['result'] = 'failed'
        receipt['error'] = str(error)
    finally:
        cleanup = []
        for package in reversed(attempted):
            try:
                command('uninstall', package)
            except Exception as error:
                receipt.setdefault('cleanup_notes', []).append(str(error))
            try:
                cleanup.append('package:' + package not in command('shell', 'pm', 'list', 'packages', package).splitlines())
            except Exception as error:
                cleanup.append(False)
                receipt.setdefault('cleanup_notes', []).append(str(error))
        receipt['packages_removed'] = all(cleanup)
        if not receipt['packages_removed']:
            receipt['result'] = 'failed'
        (evidence / (suite + '-device-receipt.json')).write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({k: receipt.get(k) for k in ('result', 'packages_removed', 'error')}))
    return 0 if receipt.get('result') == 'passed' else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--suite', choices=('store', 'source-identity', 'wake-budget'), default='store')
    args = parser.parse_args()
    raise SystemExit(qualify(args.serial, args.suite))
