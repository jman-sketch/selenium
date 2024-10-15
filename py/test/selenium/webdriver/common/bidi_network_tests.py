# Licensed to the Software Freedom Conservancy (SFC) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The SFC licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
import pytest

from selenium.webdriver.common.bidi.network import BeforeRequestSentParameters, ContinueRequestParameters


@pytest.mark.xfail_safari
@pytest.mark.xfail_firefox
@pytest.mark.xfail_opera
async def test_add_request_handler(driver):

    def request_filter(params: BeforeRequestSentParameters):
        return params.request["url"] == "https://www.example.com/"

    def request_handler(params: BeforeRequestSentParameters):
        request = params.request["request"]
        json = {
            "request": request,
            "url" : "https://www.selenium.dev/about/"
        }
        return ContinueRequestParameters(**json)

    await driver.network.add_request_handler(request_filter, request_handler)
    driver.get("https://www.example.com/")
    assert driver.current_url == "https://www.selenium.dev/about/"
