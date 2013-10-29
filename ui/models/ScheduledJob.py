class ScheduledJob (BaseJob):
    def __init__(self, *args, **kwargs):
        try:
            self.startTime = kwargs.pop('startTime')
            self.startDate = kwargs.pop('startDate')
            self.repeats = kwargs.pop('repeats')
            self.duration = kwargs.pop('duration')
            self.epsilon = kwargs.pop('epsilon')
            self.schedule = kwargs.pop('schedule')
        except KeyError:
            pass
        super(ScheduledJob, self).__init__(*args, **kwargs)
