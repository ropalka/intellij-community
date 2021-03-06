// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.python.edu;

import com.google.common.collect.Sets;
import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.intention.IntentionActionBean;
import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.GeneralSettings;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.scopeView.ScopeViewPane;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.customization.ActionUrl;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.ide.ui.customization.CustomizationUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.TipAndTrickBean;
import com.intellij.notification.EventLog;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.fileChooser.impl.FileChooserUtil;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.keymap.impl.KeymapImpl;
import com.intellij.openapi.keymap.impl.ui.Group;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import com.intellij.platform.DirectoryProjectConfigurator;
import com.intellij.platform.PlatformProjectViewOpener;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.projectImport.ProjectAttachProcessor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.tree.TreeUtil;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.codeInsight.PyCodeInsightSettings;
import com.jetbrains.python.inspections.PyPep8Inspection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author traff
 */
@SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "UtilityClassWithPublicConstructor"})
public class PyCharmEduInitialConfigurator {
  @NonNls private static final String DISPLAYED_PROPERTY = "PyCharmEDU.initialConfigurationShown";

  @NonNls private static final String CONFIGURED = "PyCharmEDU.InitialConfiguration";
  @NonNls private static final String CONFIGURED_V1 = "PyCharmEDU.InitialConfiguration.V1";
  @NonNls private static final String CONFIGURED_V2 = "PyCharmEDU.InitialConfiguration.V2";
  @NonNls private static final String CONFIGURED_V3 = "PyCharmEDU.InitialConfiguration.V3";
  @NonNls private static final String CONFIGURED_V4 = "PyCharmEDU.InitialConfiguration.V4";

  private static final Set<String> UNRELATED_TIPS = Sets.newHashSet("LiveTemplatesDjango.html", "TerminalOpen.html",
                                                                    "Terminal.html", "ConfiguringTerminal.html");
  private static final Set<String> HIDDEN_ACTIONS = ContainerUtil.newHashSet("CopyAsPlainText", "CopyAsRichText", "EditorPasteSimple",
                                                                             "Folding", "Generate", "CompareClipboardWithSelection",
                                                                             "ChangeFileEncodingAction", "CloseAllUnmodifiedEditors",
                                                                             "CloseAllUnpinnedEditors", "CloseAllEditorsButActive",
                                                                             "CopyReference", "MoveTabRight", "MoveTabDown", "External Tools",
                                                                             "MoveEditorToOppositeTabGroup", "OpenEditorInOppositeTabGroup",
                                                                             "ChangeSplitOrientation", "PinActiveTab", "Tabs Placement",
                                                                             "TabsAlphabeticalMode", "AddNewTabToTheEndMode", "NextTab",
                                                                             "PreviousTab", "Add to Favorites", "Add All To Favorites",
                                                                             "ValidateXml", "NewHtmlFile", "CleanPyc", "Images.ShowThumbnails",
                                                                             "CompareFileWithEditor", "SynchronizeCurrentFile",
                                                                             "Mark Directory As", "CompareTwoFiles", "ShowFilePath",
                                                                             "ChangesView.ApplyPatch", "TemplateProjectProperties",
                                                                             "ExportToHTML", "SaveAll", "Export/Import Actions",
                                                                             "Synchronize", "Line Separators", "ToggleReadOnlyAttribute",
                                                                             "Macros", "EditorToggleCase", "EditorJoinLines", "FillParagraph",
                                                                             "Convert Indents", "TemplateParametersNavigation", "EscapeEntities",
                                                                             "QuickDefinition", "ExpressionTypeInfo", "EditorContextInfo",
                                                                             "ShowErrorDescription", "RecentChanges", "CompareActions",
                                                                             "GotoCustomRegion", "JumpToLastChange", "JumpToNextChange",
                                                                             "SelectIn", "GotoTypeDeclaration", "QuickChangeScheme",
                                                                             "GotoTest", "GotoRelated", "Hierarchy Actions", "Bookmarks",
                                                                             "Goto Error/Bookmark Actions", "GoToEditPointGroup",
                                                                             "Change Navigation Actions", "Method Navigation Actions",
                                                                             "EvaluateExpression", "Pause", "ViewBreakpoints",
                                                                             "XDebugger.MuteBreakpoints", "SaveAs", "XDebugger.SwitchWatchesInVariables");

  public static class First {

    public First() {
      patchRootAreaExtensions();
    }
  }

  /**
   */
  public PyCharmEduInitialConfigurator(MessageBus bus,
                                       CodeInsightSettings codeInsightSettings,
                                       final PropertiesComponent propertiesComponent,
                                       FileTypeManager fileTypeManager,
                                       final ProjectManagerEx projectManager) {

    if (!propertiesComponent.getBoolean(CONFIGURED_V4)) {
      propertiesComponent.setValue(CONFIGURED_V4, true);
    }

    if (!propertiesComponent.getBoolean(CONFIGURED_V2)) {
      EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
      editorSettings.setEnsureNewLineAtEOF(true);

      propertiesComponent.setValue(CONFIGURED_V2, true);
    }
    if (!propertiesComponent.getBoolean(CONFIGURED_V1)) {
      patchMainMenu();
      UISettings.getInstance().setShowNavigationBar(false);
      propertiesComponent.setValue(CONFIGURED_V1, true);
      propertiesComponent.setValue("ShowDocumentationInToolWindow", true);
    }

    if (!propertiesComponent.getBoolean(CONFIGURED)) {
      propertiesComponent.setValue(CONFIGURED, "true");
      propertiesComponent.setValue("toolwindow.stripes.buttons.info.shown", "true");

      codeInsightSettings.REFORMAT_ON_PASTE = CodeInsightSettings.NO_REFORMAT;

      GeneralSettings.getInstance().setShowTipsOnStartup(false);

      EditorSettingsExternalizable.getInstance().setVirtualSpace(false);
      EditorSettingsExternalizable.getInstance().getOptions().ARE_LINE_NUMBERS_SHOWN = true;
      CodeStyle.getDefaultSettings().getCommonSettings(PythonLanguage.getInstance()).ALIGN_MULTILINE_PARAMETERS_IN_CALLS = true;
      final String ignoredFilesList = fileTypeManager.getIgnoredFilesList();
      ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> FileTypeManager.getInstance().setIgnoredFilesList(ignoredFilesList + ";*$py.class")));
      PyCodeInsightSettings.getInstance().SHOW_IMPORT_POPUP = false;
    }
    final EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getScheme(EditorColorsScheme.DEFAULT_SCHEME_NAME);
    editorColorsScheme.setEditorFontSize(14);

    MessageBusConnection connection = bus.connect();
    connection.subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener() {
      @Override
      public void welcomeScreenDisplayed() {
        if (!propertiesComponent.isValueSet(DISPLAYED_PROPERTY)) {
          ApplicationManager.getApplication().invokeLater(() -> {
            if (!propertiesComponent.isValueSet(DISPLAYED_PROPERTY)) {
              GeneralSettings.getInstance().setShowTipsOnStartup(false);
              patchKeymap();
              propertiesComponent.setValue(DISPLAYED_PROPERTY, "true");
            }
          });
        }
      }

      @Override
      public void appFrameCreated(String[] commandLineArgs, @NotNull Ref<Boolean> willOpenProject) {
        if (!propertiesComponent.isValueSet(CONFIGURED_V3)) {
          propertiesComponent.setValue(CONFIGURED_V3, "true");
        }
      }
    });

    connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(@NotNull final Project project) {
        if (FileChooserUtil.getLastOpenedFile(project) == null) {
          FileChooserUtil.setLastOpenedFile(project, VfsUtil.getUserHomeDir());
        }

        patchProjectAreaExtensions(project);

        StartupManager.getInstance(project).runWhenProjectIsInitialized(new DumbAwareRunnable() {
          @Override
          public void run() {
            if (project.isDisposed()) return;
            updateInspectionsProfile();
            openProjectStructure();
          }

          private void openProjectStructure() {
            ToolWindowManager.getInstance(project).invokeLater(new Runnable() {
              int count = 0;

              @Override
              public void run() {
                if (project.isDisposed()) return;
                if (count++ < 3) { // we need to call this after ToolWindowManagerImpl.registerToolWindowsFromBeans
                  ToolWindowManager.getInstance(project).invokeLater(this);
                  return;
                }
                ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Project");
                if (toolWindow != null && toolWindow.getType() != ToolWindowType.SLIDING) {
                  toolWindow.activate(null);
                }
              }
            });
          }

          private void updateInspectionsProfile() {
            final String[] codes = new String[]{"W29", "E501"};
            final VirtualFile baseDir = project.getBaseDir();
            final PsiDirectory directory = PsiManager.getInstance(project).findDirectory(baseDir);
            if (directory != null) {
              InspectionProjectProfileManager.getInstance(project).getCurrentProfile().modifyToolSettings(
                Key.<PyPep8Inspection>create(PyPep8Inspection.INSPECTION_SHORT_NAME), directory,
                inspection -> Collections.addAll(inspection.ignoredErrors, codes)
              );
            }
          }
        });
      }
    });
  }

  private static void patchMainMenu() {
    final CustomActionsSchema schema = new CustomActionsSchema();

    final JTree actionsTree = new Tree();
    Group rootGroup = new Group("root", null, null);
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootGroup);
    DefaultTreeModel model = new DefaultTreeModel(root);
    actionsTree.setModel(model);

    schema.fillActionGroups(root);
    for (int i = 0; i < root.getChildCount(); i++) {
      final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)root.getChildAt(i);
      if ("Main menu".equals(getItemId(treeNode))) {
        hideActionFromMainMenu(root, schema, treeNode);
      }
      hideActions(schema, root, treeNode, HIDDEN_ACTIONS);
    }
    CustomActionsSchema.getInstance().copyFrom(schema);
  }

  private static void hideActionFromMainMenu(@NotNull final DefaultMutableTreeNode root,
                                             @NotNull final CustomActionsSchema schema, DefaultMutableTreeNode mainMenu){
    final HashSet<String> menuItems = ContainerUtil.newHashSet("Tools", "VCS", "Refactor", "Window", "Run");
    hideActions(schema, root, mainMenu, menuItems);
  }

  private static void hideActions(@NotNull CustomActionsSchema schema, @NotNull DefaultMutableTreeNode root,
                                  @NotNull final TreeNode actionGroup, Set<String> items) {
    for(int i = 0; i < actionGroup.getChildCount(); i++){
      final DefaultMutableTreeNode child = (DefaultMutableTreeNode)actionGroup.getChildAt(i);
      final int childCount = child.getChildCount();
      final String childId = getItemId(child);
      if (childId != null && items.contains(childId)){
        final TreePath treePath = TreeUtil.getPath(root, child);
        final ActionUrl url = CustomizationUtil.getActionUrl(treePath, ActionUrl.DELETED);
        schema.addAction(url);
      }
      else if (childCount > 0) {
        hideActions(schema, child, child, items);
      }
    }
  }

  @Nullable
  private static String getItemId(@NotNull final DefaultMutableTreeNode child) {
    final Object userObject = child.getUserObject();
    if (userObject instanceof String) return (String)userObject;
    return userObject instanceof Group ? ((Group)userObject).getName() : null;
  }

  private static void patchRootAreaExtensions() {
    ExtensionsArea rootArea = Extensions.getArea(null);

    rootArea.unregisterExtensionPoint("com.intellij.runLineMarkerContributor");
    for (ToolWindowEP ep : ToolWindowEP.EP_NAME.getExtensionList()) {
      if (ToolWindowId.FAVORITES_VIEW.equals(ep.id) || ToolWindowId.TODO_VIEW.equals(ep.id) || EventLog.LOG_TOOL_WINDOW_ID.equals(ep.id)
          || ToolWindowId.STRUCTURE_VIEW.equals(ep.id)) {
        rootArea.getExtensionPoint(ToolWindowEP.EP_NAME).unregisterExtension(ep);
      }
    }

    rootArea.getExtensionPoint(DirectoryProjectConfigurator.EP_NAME).unregisterExtension(PlatformProjectViewOpener.class);

    // unregister unrelated tips
    for (TipAndTrickBean tip : TipAndTrickBean.EP_NAME.getExtensionList()) {
      if (UNRELATED_TIPS.contains(tip.fileName)) {
        rootArea.getExtensionPoint(TipAndTrickBean.EP_NAME).unregisterExtension(tip);
      }
    }

    for (IntentionActionBean ep : IntentionManager.EP_INTENTION_ACTIONS.getExtensionList()) {
      if ("org.intellij.lang.regexp.intention.CheckRegExpIntentionAction".equals(ep.className)) {
        rootArea.getExtensionPoint(IntentionManager.EP_INTENTION_ACTIONS).unregisterExtension(ep);
      }
    }

    final ExtensionPoint<ProjectAttachProcessor> point = Extensions.getRootArea().getExtensionPoint(ProjectAttachProcessor.EP_NAME);
    for (ProjectAttachProcessor attachProcessor : ProjectAttachProcessor.EP_NAME.getExtensionList()) {
      point.unregisterExtension(attachProcessor);
    }
  }

  private static void patchProjectAreaExtensions(@NotNull final Project project) {
    Executor debugExecutor = DefaultDebugExecutor.getDebugExecutorInstance();
    ActionManager.getInstance().unregisterAction(debugExecutor.getId());
    ActionManager.getInstance().unregisterAction(debugExecutor.getContextActionId());

    ExtensionsArea projectArea = Extensions.getArea(project);

    for (SelectInTarget target : SelectInTarget.EP_NAME.getExtensions(project)) {
      if (ToolWindowId.FAVORITES_VIEW.equals(target.getToolWindowId()) ||
          ToolWindowId.STRUCTURE_VIEW.equals(target.getToolWindowId())) {
        projectArea.getExtensionPoint(SelectInTarget.EP_NAME).unregisterExtension(target);
      }
    }

    for (AbstractProjectViewPane pane : AbstractProjectViewPane.EP_NAME.getExtensions(project)) {
      if (pane.getId().equals(ScopeViewPane.ID)) {
        Disposer.dispose(pane);
        projectArea.getExtensionPoint(AbstractProjectViewPane.EP_NAME).unregisterExtension(pane);
      }
    }
  }

  private static void patchKeymap() {
    Set<String> droppedActions = ContainerUtil.newHashSet(
      "AddToFavoritesPopup",
      "DatabaseView.ImportDataSources",
      "CompileDirty", "Compile",
      // hidden
      "AddNewFavoritesList", "EditFavorites", "RenameFavoritesList", "RemoveFavoritesList");
    KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();


    for (Keymap keymap : keymapManager.getAllKeymaps()) {
      if (keymap.canModify()) continue;

      KeymapImpl keymapImpl = (KeymapImpl)keymap;

      for (String id : keymapImpl.getOwnActionIds()) {
        if (droppedActions.contains(id)) keymapImpl.clearOwnActionsId(id);
      }
    }
  }
}
