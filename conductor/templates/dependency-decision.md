# Dependency decision template

- ID / owning track / date / evidence refresh date:
- Problem and requirement IDs:
- Existing library or app-owned/native implementation being assessed:
- Options: keep current, improve custom code, native API, library, defer:
- Candidate versions, licences, primary sources and compatibility matrix:
- Intended role: prototype-only / production candidate / long-term replacement:
- Replaced responsibilities and remaining platform-specific code:
- Adoption cost, maintenance, build/ABI/FFI impact and transitive dependencies:
- Prototype hypothesis, bounded effort, synthetic inputs and measured results:
- Contract, error/retry, security, performance and upgrade tests:
- Decision: adopt / retain / reject / defer / retire; rationale and uncertainty:
- Temporary dependency review/expiry trigger; removal or promotion criteria:
- Data/config/API migration, coexistence, rollback and cleanup:
- Evidence needed for production promotion and actual validation status:
- Follow-up owner track and next review trigger:

Decisions must distinguish a successful experiment from a production commitment. No dependency is approved merely by appearing in a shortlist.
