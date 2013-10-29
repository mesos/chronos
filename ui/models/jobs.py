import time

class BaseJob:
    def __init__(self,
                 name='-',
                 owner='',
                 command='-',
                 retries=2,
                 lastSuccess='',
                 lastError='',
                 successCount=0,
                 errorCount=0,
                 disabled=False):
        self.name = name
        self.owner = owner
        self.command = command
        self.retries = retries
        self.lastSuccess = lastSuccess
        self.lastError = lastError
        self.successCount = successCount
        self.errorCount = errorCount
        self.disabled = disabled
        self.stats = {}

        if self.lastSuccess and self.lastError:
            timeOfLastSuccess = time.strptime(self.lastSuccess, "%Y-%m-%dT%H:%M:%S.%fZ")
            timeOfLastError = time.strptime(self.lastError, "%Y-%m-%dT%H:%M:%S.%fZ")
            self.lastStatus = "failed" if timeOfLastError > timeOfLastSuccess else "succeeded"

        elif self.lastSuccess:
            self.lastStatus = "succeeded"

        elif self.lastError:
            self.lastStatus = "failed"

        else:
            self.lastStatus = "fresh"

class DependentJob (BaseJob):
    def __init__(self, *args, **kwargs):
        try:
            self.parents = kwargs.pop('parents')
        except KeyError:
            pass
        BaseJob.__init__(self, *args, **kwargs)

class ScheduledJob (BaseJob):
    def __init__(self, *args, **kwargs):
        try:
            self.schedule = kwargs.pop('schedule')
        except KeyError:
            pass
        BaseJob.__init__(self, *args, **kwargs)
