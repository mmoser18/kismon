/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.security;

import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class CustomRequestCache extends HttpSessionRequestCache
{
	@Override
	public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
		if (!SecurityUtils.isFrameworkInternalRequest(request)) {
			/* Saves unauthenticated requests so we can redirect the user to the page they were trying to access once they’re logged in: */
			super.saveRequest(request, response);
		}
	}
}
