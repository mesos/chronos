class DependentJob (BaseJob):
    def __init__(self, *aargs, **kwargs):
        try:
            self.parents = kwargs.pop('parents')
        except KeyError:
            pass
        super(DependentJob, self).__init__(*args, **kwargs)
