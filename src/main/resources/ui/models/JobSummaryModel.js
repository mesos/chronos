import {observable, computed} from 'mobx';

export default class JobSummaryModel {
  name;
  @observable status;
  @observable state;
  @observable schedule;
  @observable parents;
  @observable disabled;

  constructor(store, name, status, state, schedule, parents, disabled) {
    this.store = store
    this.name = name
    this.status = status
    this.state = state
    this.schedule = schedule
    this.parents = parents
    this.disabled = disabled
  }

  @computed get nextExpected() {
    if (!this.schedule) {
      return "—"
    } else {
      var scheduledDate = Date.parse(this.schedule.split("/")[1])
      var dateDiff = (scheduledDate - new Date().getTime()) / 1000.
      if (dateDiff <= 1) {
        return "OVERDUE"
      } else if (dateDiff <= 3600) {
        var minutes = Math.ceil(dateDiff / 60.0)
        if (minutes == 1) {
          return "in <" + minutes + " minute"
        } else {
          return "in ~" + minutes + " minutes"
        }
      } else if (dateDiff <= 86400) {
        var hours = Math.ceil(dateDiff / 3600.0)
        return "in ~" + hours + " hours"
      } else {
        var days = Math.ceil(dateDiff / 86400.0)
        return "in ~" + days + " days"
      }
    }
  }

  destroy() {
    this.store.removeJobSummary(this)
  }

  updateFromJson(json) {
    this.name = json.name
    this.status = json.status
    this.state = json.state
    this.schedule = json.schedule
    this.parents = json.parents
    this.disabled = json.disabled
  }

  static fromJS(store, json) {
    return new JobSummaryModel(
      store,
      json.name,
      json.state,
      json.status,
      json.schedule,
      json.parents,
      json.disabled
    )
  }
}
