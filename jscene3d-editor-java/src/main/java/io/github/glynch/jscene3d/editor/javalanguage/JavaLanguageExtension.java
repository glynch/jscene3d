/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage;

import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import io.github.glynch.jscene3d.editor.javalanguage.jdt.JdtLanguageServerStatus;
import io.github.glynch.jscene3d.editor.javalanguage.jdt.JdtLanguageSupportFactory;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportContribution;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportId;
import io.github.glynch.jscene3d.editor.status.EditorStatusItem;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.status.StatusBarAlignment;
import io.github.glynch.jscene3d.editor.status.StatusItemId;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.resourcebundle.ResourceBundleMessageSource;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/** Built-in extension contributing project-scoped Java support backed by Eclipse JDT LS. */
public final class JavaLanguageExtension implements EditorExtension {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.editor.builtin.java";
    private static final EditorLanguageSupportId SUPPORT_ID = new EditorLanguageSupportId(EXTENSION_ID + ".jdtls");
    private static final StatusItemId STATUS_ID = new StatusItemId(EXTENSION_ID + ".status");
    private static final DiagnosticCollectionId DIAGNOSTICS_ID =
            new DiagnosticCollectionId(EXTENSION_ID + ".diagnostics");
    private static final String MESSAGE_BUNDLE = "io.github.glynch.jscene3d.editor.javalanguage.messages";

    private final String serverVersion;
    private final MessageSource messages;
    private final ExecutorService executor;
    private final AtomicReference<EditorStatusItem> statusItem = new AtomicReference<>();

    /**
     * Creates Java support from the checksum-verified distribution staged by the editor build.
     *
     * @param executor caller-owned executor for JDT LS startup and protocol work
     */
    public JavaLanguageExtension(ExecutorService executor) {
        this(new ResourceBundleMessageSource(JavaLanguageExtension.class.getModule(), MESSAGE_BUNDLE), executor);
    }

    private JavaLanguageExtension(MessageSource messages, ExecutorService executor) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.serverVersion = JdtLanguageSupportFactory.serverVersion();
    }

    @Override
    public String id() {
        return EXTENSION_ID;
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(),
                "Java Language Support",
                "Provides Java project intelligence using Eclipse JDT Language Server.",
                "JScene3D",
                Optional.of(serverVersion),
                true);
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        EditorStatusItem item = editor.subscriptions()
                .add(editor.statusBar()
                        .create(new EditorStatusItemContribution(STATUS_ID, StatusBarAlignment.LEFT, 100)));
        item.update(statusPresentation(JdtLanguageServerStatus.inactive()));
        statusItem.set(item);
        editor.subscriptions().add(() -> statusItem.compareAndSet(item, null));
        EditorDiagnosticCollection diagnostics =
                editor.subscriptions().add(editor.diagnostics().createCollection(DIAGNOSTICS_ID));
        var support = JdtLanguageSupportFactory.create(this::publishStatus, diagnostics, executor)
                .support();
        editor.subscriptions()
                .add(editor.languageSupports()
                        .register(
                                new EditorLanguageSupportContribution(SUPPORT_ID, Set.of(EditorLanguages.JAVA)),
                                support));
    }

    private void publishStatus(JdtLanguageServerStatus status) {
        EditorStatusItem item = statusItem.get();
        if (item != null) {
            item.update(statusPresentation(status));
        }
    }

    private EditorStatusItemState statusPresentation(JdtLanguageServerStatus status) {
        StatusCopy copy =
                switch (status.state()) {
                    case INACTIVE ->
                        new StatusCopy(
                                "java.status.inactive", "Java: Inactive", "No Java language-server session is active");
                    case STARTING ->
                        new StatusCopy(
                                "java.status.starting", "Java: Starting…", "Initializing Eclipse JDT Language Server");
                    case READY ->
                        new StatusCopy("java.status.ready", "Java: Ready", "Eclipse JDT Language Server is ready");
                    case FAILED ->
                        new StatusCopy(
                                "java.status.failed",
                                "Java: Failed",
                                "Eclipse JDT Language Server failed to initialize");
                };
        Locale locale = Locale.getDefault(Locale.Category.DISPLAY);
        String text = messages.getMessage(copy.code(), copy.text(), locale);
        String tooltip =
                status.detail().orElseGet(() -> messages.getMessage(copy.code() + ".tooltip", copy.tooltip(), locale));
        return new EditorStatusItemState(
                text,
                Optional.of(new EditorIcon(EditorIcons.JAVA, tooltip)),
                Optional.of(tooltip),
                Optional.empty(),
                status.state() != JdtLanguageServerStatus.State.INACTIVE);
    }

    private record StatusCopy(String code, String text, String tooltip) {}
}
