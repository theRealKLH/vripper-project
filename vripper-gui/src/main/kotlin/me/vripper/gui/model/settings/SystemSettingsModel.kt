package me.vripper.gui.model.settings

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getValue
import tornadofx.setValue

class SystemSettingsModel {
    val tempPathProperty = SimpleStringProperty()
    var tempPath: String by tempPathProperty

    val logEntriesProperty = SimpleIntegerProperty()
    var logEntries: Int by logEntriesProperty

    val enableProperty = SimpleBooleanProperty()
    var enable: Boolean by enableProperty

    val pollingRateProperty = SimpleIntegerProperty()
    var pollingRate: Int by pollingRateProperty
}
