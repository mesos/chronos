class BaseJob:
    def __init__(self, 
                 name='-', 
                 owner='', 
                 command='-',
                 epsilon='PT15M',
                 retries=2,
                 lastSuccess='-',
                 lastError='-',
                 successCount=0,
                 errorCount=0,
                 persisted=False,
                 async=False,
                 disabled=False):
        self.name = name
        self.owner = owner
        self.command = command
        self.epsilon = epsilon
        self.retries = retries
        self.lastSuccess = lastSuccess
        self.lastError = lastError
        self.successCount = successCount
        self.errorCount = errorCount
        self.persisted = persisted
        self.async = async
        self.disabled = disabled
