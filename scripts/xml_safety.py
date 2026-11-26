"""Parse XML without accepting document type declarations."""
import xml.etree.ElementTree as ET


class _NoDTDTreeBuilder(ET.TreeBuilder):
    def doctype(self, name, public_id, system_id):
        raise ET.ParseError('DTD declarations are not allowed')


def _parser():
    return ET.XMLParser(target=_NoDTDTreeBuilder())


def parse_xml(data):
    """Keep normal ElementTree parsing while rejecting DTDs before expansion."""
    return ET.fromstring(data, parser=_parser())


def parse_xml_file(path):
    """Parse a file incrementally with the same DTD restriction."""
    return ET.parse(path, parser=_parser()).getroot()
