"""Validate every HTTP redirect before contacting the next destination."""
import urllib.request


class ValidatingRedirectHandler(urllib.request.HTTPRedirectHandler):
    def __init__(self, validate):
        super().__init__()
        self.validate = validate

    def redirect_request(self, request, response, code, message, headers, new_url):
        try:
            self.validate(new_url)
        except Exception:
            response.close()
            raise
        return super().redirect_request(request, response, code, message, headers, new_url)
