/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 12 Feb 2021
 */

package net.mmo.utils.kism.ui.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import lombok.extern.slf4j.Slf4j;

/**
 * Shamelessly stolen from here:
 * https://github.com/vaadin/vaadin-split-layout/issues/152
 */
@Slf4j
public class SplitLayoutEnhanced extends SplitLayout
{
	private static final long serialVersionUID = 4911977866777913424L;

	private static final String PRIMARY_OFFSET_HEIGHT   = "element._primaryChild.offsetHeight"; //$NON-NLS-1$
	private static final String SECONDARY_OFFSET_HEIGHT = "element._secondaryChild.offsetHeight"; //$NON-NLS-1$
	private static final String PRIMARY_OFFSET_WIDTH    = "element._primaryChild.offsetWidth"; //$NON-NLS-1$
	private static final String SECONDARY_OFFSET_WIDTH  = "element._secondaryChild.offsetWidth"; //$NON-NLS-1$

	private String PRIMARY_SIZE   = PRIMARY_OFFSET_WIDTH;
	private String SECONDARY_SIZE = SECONDARY_OFFSET_WIDTH;

	private double splitterPosition;
	private double primarySizePixel;
	private double secondarySizePixel;

	public SplitLayoutEnhanced() {
		super();
		init();
	}

	public SplitLayoutEnhanced(Component primaryComponent,
	                           Component secondaryComponent) {
		super(primaryComponent, secondaryComponent);
		init();
	}

	private void init() {
		getElement()
			.addEventListener("splitter-dragend", e -> { //$NON-NLS-1$
				this.primarySizePixel	= e.getEventData().getNumber(this.PRIMARY_SIZE);
				this.secondarySizePixel	= e.getEventData().getNumber(this.SECONDARY_SIZE);
				double totalSize = this.primarySizePixel + this.secondarySizePixel;
				this.splitterPosition = Math.round(this.primarySizePixel / totalSize * 100.0);
				log.debug("splitter-dragend: {} | {} (={}) --> splitterpos: {}%", this.primarySizePixel, this.secondarySizePixel, totalSize, this.splitterPosition); //$NON-NLS-1$
			})
			.addEventData(this.PRIMARY_SIZE)
			.addEventData(this.SECONDARY_SIZE);
	}

	@Override
	public void setOrientation(Orientation orientation) {
		super.setOrientation(orientation);

		if (orientation == Orientation.HORIZONTAL) {
			this.PRIMARY_SIZE	= PRIMARY_OFFSET_WIDTH;
			this.SECONDARY_SIZE	= SECONDARY_OFFSET_WIDTH;
		} else {
			this.PRIMARY_SIZE	= PRIMARY_OFFSET_HEIGHT;
			this.SECONDARY_SIZE	= SECONDARY_OFFSET_HEIGHT;
		}
	}

	// What a brain-damaged numbering! The value is to be given in percent
	// of the width of the secondary component. Thus 100 means "in the middle".
	// One would have guessed that's 50(%) but not so...
	@Override
	public void setSplitterPosition(final double position) {
		log.debug("setSplitterPosition({})", position); //$NON-NLS-1$
		super.setSplitterPosition(position);
		this.splitterPosition = position;
	}

	public double getSplitterPosition() {
		log.debug("getSplitterPosition(): {}%", this.splitterPosition); //$NON-NLS-1$
		return this.splitterPosition;
	}

	public double getPrimarySizePixel() {
		return this.primarySizePixel;
	}

	public double getSecondarySizePixel() {
		return this.secondarySizePixel;
	}
}