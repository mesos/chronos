import {observable, computed} from 'mobx';

export default class JsonModel {
  name
  @observable json

  constructor(store, name, json) {
    this.store = store
    this.name = name

    // remove specific fields
    delete json['lastError']
    delete json['lastSuccess']
    delete json['errorsSinceLastSuccess']
    delete json['successCount']
    delete json['errorCount']

    this.json = json
  }

  static fromJS(store, json) {
    return new JsonModel(
      store,
      json.name,
      json
    )
  }
}
