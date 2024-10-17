from dataclasses import fields

from selenium.webdriver.common.bidi import network
from selenium.webdriver.common.bidi.network import (
    AddInterceptParameters,
    BeforeRequestSent,
    BeforeRequestSentParameters,
    ContinueRequestParameters,
)


def default_request_handler(params: BeforeRequestSentParameters):
    return ContinueRequestParameters(request=params.request["request"])


class Network:
    def __init__(self, driver):
        self.network = None
        self.driver = driver
        self.intercept = None

    async def add_request_handler(self, request_filter=lambda _: True, handler=default_request_handler, conn=None):
        self.network = network.Network(conn)
        params = AddInterceptParameters(["beforeRequestSent"])
        callback = self._callback(request_filter, handler)
        result = await self.network.add_intercept(event=BeforeRequestSent, params=params)
        intercept = result["intercept"]
        self.intercept = intercept
        await self.network.add_listener(event=BeforeRequestSent, callback=callback)
        return intercept

    def _callback(self, request_filter, handler):
        async def callback(request):
            if request_filter(request):
                request = handler(request)
            else:
                request = default_request_handler(request)
            await self.network.continue_request(request)

        return callback
