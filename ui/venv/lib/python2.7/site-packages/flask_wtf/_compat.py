import sys
if sys.version_info[0] == 3:
    text_type = str
    string_types = (str,)
else:
    text_type = unicode
    string_types = (str, unicode)


def to_bytes(text):
    """Transform string to bytes."""
    if isinstance(text, text_type):
        text = text.encode('utf-8')
    return text
