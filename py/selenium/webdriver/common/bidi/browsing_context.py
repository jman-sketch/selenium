import re
import typing
from dataclasses import dataclass, fields, is_dataclass

from selenium.webdriver.common.bidi.cdp import import_devtools

devtools = import_devtools("")
event_class = devtools.util.event_class

@dataclass
class NavigateParameters:
    context: str
    url: str
    wait: str = "complete"

    def to_json(self):
        json = {}
        for field in fields(self):
            key = field.name
            value = getattr(self, key)
            if not value:
                continue
            if is_dataclass(value):
                value = value.to_json()
            json = json | {re.sub(r"^_", "", key): value}
        return json

    @classmethod
    def from_json(cls, json):
        return cls(**json)

@dataclass
class Navigate:
    params: NavigateParameters
    method: typing.Literal["browsingContext.navigate"] = "browsingContext.navigate"

    def to_json(self):
        json = {}
        for field in fields(self):
            key = field.name
            value = getattr(self, key)
            if not value:
                continue
            if is_dataclass(value):
                value = value.to_json()
            json = json | {re.sub(r"^_", "", key): value}
        return json

    @classmethod
    def from_json(cls, json):
        return cls(**json)

    def cmd(self):
        result = yield self.to_json()
        return result