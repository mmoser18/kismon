/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17.10.2020
 */

package net.mmo.utils.kism.ui.view.results;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ListDataProvider;
import net.mmo.utils.kism.entities.results.RequestResults;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

//To test a class that uses @Autowire, a database, or any other feature provided by Spring
//Boot, you can no longer use plain JUnit tests. Instead, you can use the Spring Boot test
//runner. This does add a little overhead, but it makes more features available to your test.
//
//The @RunWith(SpringRunner.class) (for JUnit4)/@ExtendWith(SpringExtension.class) (for JUnit5)
// and @SpringBootTest annotations make sure that the Spring Boot application is initialized
// before the tests are run and allow you to use @Autowired in the test.
@ExtendWith(SpringExtension.class)
@SpringBootTest // makes sure that the Spring Boot application is initialized before the tests are run and allows you to use @Autowire in the test.
public class ResultViewTest
{
	@Autowired
	private ResultView listView;

	// @Test
	public void formShownWhenContactSelected() {
		Grid<RequestResults> grid = this.listView.grid;
		@SuppressWarnings("unused")
		RequestResults firstResult = getFirstItem(grid);
		// to be completed...
	}

	@SuppressWarnings({"unchecked", "static-method"})
	private RequestResults getFirstItem(Grid<RequestResults> grid) {
		return ((ListDataProvider<RequestResults>)grid.getDataProvider()).getItems().iterator().next();
	}
}

