from dataclasses import fields

from selenium.webdriver.common.bidi import network
from selenium.webdriver.common.bidi.cdp import open_cdp
from selenium.webdriver.common.bidi.network import (
    AddInterceptParameters,
    BeforeRequestSent,
    BeforeRequestSentParameters,
    ContinueRequestParameters,
)


def default_request_handler(params: BeforeRequestSentParameters):
    return ContinueRequestParameters(request=params.request["request"])


class Network:
    def __init__(self, ws_url):
        self.ws_url = ws_url
        self.network = None

    async def add_request_handler(self, request_filter=lambda _: True, handler=default_request_handler):
        async with open_cdp(self.ws_url) as conn:
            if not self.network:
                self.network = network.Network(conn)
            params = AddInterceptParameters(["beforeRequestSent"])
            result = await self.network.add_intercept(event=BeforeRequestSent, params=params)
            intercept = result["intercept"]
            callback = self._callback(request_filter, handler)
            await self.network.add_listener(BeforeRequestSent, callback)
            return intercept

    def _callback(self, request_filter, handler):
        async def callback(request):
            if request_filter(request):
                request = handler(request)
            else:
                request = default_request_handler(request)
            await self.network.continue_request(request)

        return callback
