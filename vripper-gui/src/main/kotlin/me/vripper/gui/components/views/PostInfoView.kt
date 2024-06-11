package me.vripper.gui.components.views

import javafx.collections.FXCollections
import javafx.scene.control.TabPane
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import me.vripper.delegate.LoggerDelegate
import me.vripper.gui.controller.PostController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.model.PostModel
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class PostInfoView : View() {
    private val logger by LoggerDelegate()
    private val postController: PostController by inject()
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob())
    private val imagesTableView: ImagesTableView by inject()
    private val jobs = mutableListOf<Job>()
    private val postModel: PostModel = PostModel(
        -1, "", 0.0, "", "", 0, 0, "", "", -1, "", "", "", emptyList(), emptyList(), ""
    )

    override val root = tabpane()

    init {
        coroutineScope.launch {
            GuiEventBus.events.filterIsInstance(GuiEventBus.ChangingSession::class).collect {
                jobs.forEach { it.cancel() }
                jobs.clear()
            }
        }
        with(root) {
            id = "postinfo_panel"
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tab("General") {
                graphic = FontIcon(Feather.INFO)
                scrollpane {
                    form {
                        fieldset {
                            field("Post Link:") {
                                textfield(postModel.urlProperty) {
                                    isEditable = false
                                    visibleWhen(postModel.urlProperty.isNotEmpty)
                                }
                            }
                            field("Posted By:") {
                                label(postModel.postedByProperty)
                            }
                            field("Title:") {
                                label(postModel.titleProperty)
                            }
                            field("More titles:") {
                                label(postModel.altTitlesProperty.map { it.joinToString(", ") })
                            }
                            field("Status:") {
                                label(postModel.statusProperty.map { it ->
                                    it.lowercase().replaceFirstChar { it.uppercase() }
                                })
                            }

                            field("Path:") {
                                label(postModel.pathProperty)
                            }
                            field("Total:") {
                                label(postModel.progressCountProperty)
                            }
                            field("Hosts:") {
                                label(postModel.hostsProperty)
                            }

                            field("Added On:") {
                                label(postModel.addedOnProperty)
                            }
                        }
                    }
                }
            }
            tab("Photos") {
                graphic = FontIcon(Feather.IMAGE)
                add(imagesTableView)
            }
        }
    }

    fun setPostId(postId: Long?) {
        jobs.forEach { it.cancel() }
        jobs.clear()
        imagesTableView.setPostId(postId)
        if (postId == null) {
            postModel.apply {
                this.postId = -1
                this.title = ""
                this.progress = 0.0
                this.status = ""
                this.url = ""
                this.done = 0
                this.total = 0
                this.hosts = ""
                this.addedOn = ""
                this.order = -1
                this.path = ""
                this.folderName = ""
                this.progressCount = ""
                this.previewList.clear()
                this.altTitles.clear()
                this.postedBy = ""
            }
            return
        }
        coroutineScope.launch {
            val model: PostModel? = async {
                try {
                    postController.find(postId)
                } catch (e: Exception) {
                    null
                }
            }.await()
            if (model == null) {
                return@launch
            }
            runLater {
                postModel.apply {
                    this.postId = model.postId
                    this.title = model.title
                    this.progress = model.progress
                    this.status = model.status.lowercase().replaceFirstChar { it.uppercase() }
                    this.url = model.url
                    this.done = model.done
                    this.total = model.total
                    this.hosts = model.hosts
                    this.addedOn = model.addedOn
                    this.order = model.order
                    this.path = model.path
                    this.folderName = model.folderName
                    this.progressCount = model.progressCount
                    this.previewList = model.previewList
                    this.altTitles = model.altTitles
                    this.postedBy = model.postedBy
                }
            }
        }
        coroutineScope.launch {
            postController.onUpdatePosts().catch {
                logger.error("gRPC error", it)
                currentCoroutineContext().cancel(null)
            }.filter {
                it.postId == postModel.postId
            }.collect { post ->
                runLater {
                    postModel.status = post.status.stringValue.lowercase().replaceFirstChar { it.uppercase() }
                    postModel.progressCount = postController.progressCount(
                        post.total, post.done, post.downloaded
                    )
                    postModel.order = post.rank + 1
                    postModel.done = post.done
                    postModel.progress = postController.progress(
                        post.total, post.done
                    )
                    postModel.path = post.getDownloadFolder()
                    postModel.folderName = post.folderName
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            postController.onUpdateMetadata().catch {
                logger.error("gRPC error", it)
                currentCoroutineContext().cancel(null)
            }.filter {
                it.postId == postModel.postId
            }.collect {
                runLater {
                    postModel.altTitles = FXCollections.observableArrayList(it.data.resolvedNames)
                    postModel.postedBy = it.data.postedBy
                }
            }
        }.also { jobs.add(it) }
    }
}
