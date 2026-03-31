/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.utils;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Extension of anchor, that will display a vaadin button as clickable instance
 * to initiate a download. The download content is generated at click time.
 *
 * This button was originally copied from here:
 * https://vaadin.com/directory/component/lazy-download-button
 * and then substantially reworked to allow calling the getFilename() only once and to handle exceptions
 * properly.
 */
@Setter
@Getter
@Slf4j
@SuppressWarnings("javadoc")
public class LazyDownloadButton extends Button
{
	private static final long serialVersionUID = -8551403604410473130L;

	private static final String DEFAULT_FILE_NAME = "download"; //$NON-NLS-1$
	private static final Supplier<String> DEFAULT_FILE_NAME_SUPPLIER = () -> DEFAULT_FILE_NAME;
	private Supplier<String> fileNameCallback;
	private Supplier<InputStream> inputStreamCallback;

	private Anchor anchor;

	public LazyDownloadButton() {
		// empty
	}

	public LazyDownloadButton(String text) {
		super(text);
	}

	public LazyDownloadButton(Component icon) {
		super(icon);
	}

	public LazyDownloadButton(String text,
	                          Supplier<InputStream> inputStreamFactory) {
		this(text, DEFAULT_FILE_NAME_SUPPLIER, inputStreamFactory);
	}

	public LazyDownloadButton(Component icon,
	                          Supplier<InputStream> inputStreamFactory) {
		this(icon, DEFAULT_FILE_NAME_SUPPLIER, inputStreamFactory);
	}

	public LazyDownloadButton(String text, Component icon,
	                          Supplier< InputStream> inputStreamFactory) {
		this(text, icon, DEFAULT_FILE_NAME_SUPPLIER, inputStreamFactory);
	}

	public LazyDownloadButton(String text,
	                          Supplier<String> fileNameCallback,
	                          Supplier<InputStream> inputStreamFactory) {
		this(text, null, fileNameCallback, inputStreamFactory);
	}

	public LazyDownloadButton(Component icon,
	                          Supplier<String> fileNameCallback,
	                          Supplier<InputStream> inputStreamFactory) {
		this("", icon, fileNameCallback, inputStreamFactory); //$NON-NLS-1$
	}

	/**
	 * Creates a download button. The first two parameters are used for the
	 * button displayment.
	 * <p/>
	 * The third callback is called, when the download button is clicked. This
	 * is called inside the UI thread before the input stream generation starts.
	 * It can be used for instance to deactivate the button.
	 * <p/>
	 * The fourth parameter is a callback, that is used to generate the download
	 * file name
	 * <p/>
	 * The fifth parameter is a callback to generate the input stream sent to
	 * the client. This callback will be called in a separate thread (so that
	 * the UI thread is not blocked).
	 * <p/>
	 * The sixth callback is called when the download anchor on the client side
	 * has been clicked (means the input stream content is now sent to the
	 * user). This callback is called inside the UI thread. BE AWARE THAT THE
	 * FILE IS STILL SENT TO THE CLIENT AT THIS POINT. Deleting or removing
	 * things can interrupt the download.
	 *
	 * @param text button text
	 * @param icon button icon
	 * @param fileNameCallback callback for file name generation
	 * @param inputStreamCallback callback for input stream generation
	 */
	@SuppressWarnings({"removal", "resource"})
	public LazyDownloadButton(String text, Component icon,
	                          Supplier<String> fileNameCallback,
	                          Supplier<InputStream> inputStreamCallback) {
		super(text);

		Objects.requireNonNull(fileNameCallback, "File name callback must not be null"); //$NON-NLS-1$
		Objects.requireNonNull(	inputStreamCallback,
								"Input stream callback must not be null"); //$NON-NLS-1$
		this.fileNameCallback		= fileNameCallback;
		this.inputStreamCallback	= inputStreamCallback;
		if (icon != null) {
			setIcon(icon);
		}

		super.addClickListener(_ -> {
			log.info("Download button clicked"); //$NON-NLS-1$
			String filename = fileNameCallback.get();
			if (filename == null) {
				log.info("Filename for Download yielded null"); //$NON-NLS-1$
				fireEvent(new DownloadAbortedEvent(this, false, "No filename obtained.")); //$NON-NLS-1$
			} else {
				log.info("Filename for Download: '{}'", filename); //$NON-NLS-1$
				// we add the anchor to download in the parent of the button - if there are scenarios
				// where the anchor should be placed somewhere else, this needs to be extended.
				// Cannot be placed inside of the button since the button might be disabled or invisible
				// thus makes the anchor not usable.
				// The anchor must not be removed by this component, since the download fails otherwise
				getParent().ifPresent(component -> {
					if (this.anchor == null) {
						log.info("Preparing anchor..."); //$NON-NLS-1$
						this.anchor = new Anchor();
						Element anchorElement = this.anchor.getElement();
						anchorElement.setAttribute("download", true); //$NON-NLS-1$
						anchorElement.getStyle().set("display", "none"); //$NON-NLS-1$ //$NON-NLS-2$
						component.getElement().appendChild(this.anchor.getElement());
						anchorElement.addEventListener("click", //$NON-NLS-1$
						                               event1 ->
						                               {
						                                	fireEvent(new DownloadStartsEvent(this,
						                                	                                  true,
						                                	                                  event1));
						                               });
					}

					Optional<UI> optionalUI = getUI();
					Executors.newSingleThreadExecutor().execute(() -> {
						try {
							// Note: the stream must NOT be defined in the // try (...)-clause
							// here, because then it gets closed too early!
							InputStream inputStream = inputStreamCallback.get();
							if (inputStream != null) {
								log.info("Download-Inputstream obtained for '{}'", filename); //$NON-NLS-1$
								StreamResource href = new StreamResource(filename, () -> inputStream);
								optionalUI.ifPresent(ui -> ui.access(() -> {
									href.setCacheTime(0);
									this.anchor.setHref(href);
									log.info("Download stream obtained - starting download:"); //$NON-NLS-1$
									this.anchor.getElement().callJsFunction("click"); //$NON-NLS-1$
								}));
							} else {
								log.info("Download stream yielded null - download aborted..."); //$NON-NLS-1$
								fireEvent(new DownloadAbortedEvent(this, false, "No file content obtained.")); //$NON-NLS-1$
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					});
				});
			}
		});
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		if (this.anchor != null) {
			getParent().map(Component::getElement).ifPresent(parentElement -> {
				Element anchorElement = this.anchor.getElement();
				if (anchorElement != null
					&& parentElement.getChildren().anyMatch(anchorElement::equals)) {
					parentElement.removeChild(anchorElement);
				}
			});
		}
	}

	public Registration addDownloadStartsListener(ComponentEventListener<DownloadStartsEvent> listener) {
		return addListener(DownloadStartsEvent.class, listener);
	}
	public Registration addDownloadAbortedListener(ComponentEventListener<DownloadAbortedEvent> listener) {
		return addListener(DownloadAbortedEvent.class, listener);
	}

	public static class DownloadStartsEvent extends ComponentEvent<LazyDownloadButton>
	{
		private static final long serialVersionUID = 7821051986207777957L;
		private final DomEvent clientSideEvent;

		/**
		 * Creates a new event using the given source and indicator whether the
		 * event originated from the client side or the server side.
		 *
		 * @param source the source component
		 * @param fromClient <code>true</code> if the event originated from the
		 *            client
		 */
		public DownloadStartsEvent(LazyDownloadButton source, boolean fromClient,
									DomEvent clientSideEvent) {
			super(source, fromClient);
			this.clientSideEvent = clientSideEvent;
		}

		public DomEvent getClientSideEvent() {
			return this.clientSideEvent;
		}
	}
	public static class DownloadAbortedEvent extends ComponentEvent<LazyDownloadButton>
	{
		private static final long serialVersionUID = 7821051986207777957L;
		private final String reason;

		/**
		 * Creates a new event using the given source and indicator whether the
		 * event originated from the client side or the server side.
		 *
		 * @param source the source component
		 * @param fromClient <code>true</code> if the event originated from the
		 *            client
		 */
		public DownloadAbortedEvent(LazyDownloadButton source, boolean fromClient, String reason) {
			super(source, fromClient);
			this.reason = reason;
		}

		public String getReason() {
			return this.reason;
		}
	}
}
