/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */
/**
 * inspired by:
 * https://stackoverflow.com/questions/58009033/how-spring-data-elasticsearch-use-offset-and-limit-to-query/63911746#63911746
 */

package net.mmo.utils.kism.utils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * class that implements offset and limit for queries (Spring only supports paging ||-( )
 */
public class OffsetLimitPageable implements Pageable
{
	private int offset;
	private int limit;
	private Sort sort = Sort.unsorted();

	/**
	 * That's the purpose of the entire class
	 * @param offset
	 * @param limit
	 */
	public OffsetLimitPageable(int offset, int limit) {
		setOffset(offset);
		setLimit(limit);
	}

	private void setOffset(int offset) {
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must not be less than zero!"); //$NON-NLS-1$
		}
		this.offset	= offset;
	}
	private void setLimit(int limit) {
		if (limit < 1) {
			throw new IllegalArgumentException("Limit must not be less than one!"); //$NON-NLS-1$
		}
		this.limit = limit;
	}

	/**
	 * Wraps the constructor to make it nicer readable
	 * @param offset
	 * @param limit
	 * @return OffsetLimitPageable
	 */
	public static OffsetLimitPageable of(int offset, int limit) {
		return new OffsetLimitPageable(offset, limit);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getPageNumber()
	 */
	@Override
	public int getPageNumber() {
		return this.offset / this.limit;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getPageSize()
	 */
	@Override
	public int getPageSize() {
		return this.limit;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getOffset()
	 */
	@Override
	public long getOffset() {
		return this.offset;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getSort()
	 */
	@Override
	public Sort getSort() {
		return this.sort;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#next()
	 */
	@Override
	public Pageable next() {
		// Typecast possible because number of entries cannot be bigger than integer (primary key is integer)
		return of((int) (getOffset() + getPageSize()), getPageSize());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#previousOrFirst()
	 */
	@Override
	public Pageable previousOrFirst() {
		return hasPrevious() ? previous() : first();
	}

	/**
	 * @return the previous page if any, else the current is returned
	 */
	public Pageable previous() {
		// The integers are positive. Subtracting does not let them become bigger than integer.
		return hasPrevious()
			? of((int)(getOffset() - getPageSize()), getPageSize())
			: this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#first()
	 */
	@Override
	public Pageable first() {
		return of(0, getPageSize());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#hasPrevious()
	 */
	@Override
	public boolean hasPrevious() {
		return this.offset > this.limit;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#withPage()
	 */
	@Override
	public Pageable withPage(int pageNumber) {
		throw new java.lang.UnsupportedOperationException("withPage(int) not supported by " + this.getClass()); //$NON-NLS-1$
	}
}