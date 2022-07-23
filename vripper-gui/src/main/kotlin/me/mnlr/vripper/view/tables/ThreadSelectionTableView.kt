package me.mnlr.vripper.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import me.mnlr.vripper.controller.ThreadController
import me.mnlr.vripper.model.ThreadSelectionModel
import tornadofx.*

class ThreadSelectionTableView : Fragment("Thread") {

    private lateinit var tableView: TableView<ThreadSelectionModel>
    private val threadController: ThreadController by inject()
    private var items: ObservableList<ThreadSelectionModel> = FXCollections.observableArrayList()
    val threadId: String by param()

    override fun onDock() {
        tableView.prefWidthProperty().bind(root.widthProperty())
        tableView.prefHeightProperty().bind(root.heightProperty())
        modalStage?.width = 600.0
        tableView.placeholder = Label("Loading")

        runLater {
            items.addAll(threadController.grab(threadId))
        }
    }

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        spacing = 5.0
        tableView = tableview(items) {
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            setRowFactory {
                val tableRow = TableRow<ThreadSelectionModel>()

                tableRow.setOnMouseClicked {
                    if (it.clickCount == 2 && tableRow.item != null) {
                        threadController.download(listOf(tableRow.item))
                        close()
                    }
                }

                tableRow
            }
            column("Post Index", ThreadSelectionModel::indexProperty) {
                sortOrder.add(this)
            }
            column("Title", ThreadSelectionModel::titleProperty) { prefWidth = 200.0 }
            column("URL", ThreadSelectionModel::urlProperty) { prefWidth = 200.0 }
            column("Hosts", ThreadSelectionModel::hostsProperty)
        }
        borderpane {
            right {
                padding = insets(top = 0, right = 5, bottom = 5, left = 5)
                button("Download") {
                    imageview("download.png") {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                    tooltip("Download selected posts")
                    enableWhen { tableView.selectionModel.selectedItems.sizeProperty.greaterThan(0) }
                    action {
                        threadController.download(tableView.selectionModel.selectedItems)
                        close()
                    }
                }
            }
        }
    }
}