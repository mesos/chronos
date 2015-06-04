package org.apache.mesos.chronos.scheduler.jobs.constraints

import org.apache.mesos.Protos

trait ConstraintSpecHelper {
  def createTextAttribute(name: String, value: String): Protos.Attribute = {
    Protos.Attribute.newBuilder()
      .setName(name)
      .setText(Protos.Value.Text.newBuilder().setValue(value))
      .setType(Protos.Value.Type.TEXT)
      .build()
  }

  def createScalarAttribute(name: String, value: Double): Protos.Attribute = {
    Protos.Attribute.newBuilder()
      .setName(name)
      .setScalar(Protos.Value.Scalar.newBuilder().setValue(value))
      .setType(Protos.Value.Type.SCALAR)
      .build()
  }

  def createSetAttribute(name: String, value: Array[String]): Protos.Attribute = {
    val set = Protos.Attribute.newBuilder()
      .setName(name)
      .setType(Protos.Value.Type.SET)

    val builder = Protos.Value.Set.newBuilder()
    value.foreach(builder.addItem)
    set.setSet(builder)

    set.build()
  }
}
