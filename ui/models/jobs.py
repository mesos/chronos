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
            timeOfLastSuccess = time.strptime(self.lastSuccess, "%Y-%m-%d %H:%M:%S")
            timeOfLastError = time.strptime(self.lastError, "%Y-%m-%d %H:%M:%S")
            self.lastStatus = "Failed" if timeOfLastError >= timeOfLastSuccess else "Succeeded"

        elif self.lastSuccess:
            self.lastStatus = "Succeeded"

        elif self.lastError:
            self.lastStatus = "Failed"

        else:
            self.lastStatus = "Fresh"

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
