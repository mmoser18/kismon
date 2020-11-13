/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17.10.2020
 */

package net.mmo.utils.kism.ui.view.list;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ListDataProvider;
import net.mmo.utils.kism.entities.Contact;
import net.mmo.utils.kism.ui.view.list.ContactForm;
import net.mmo.utils.kism.ui.view.list.ListView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
public class ListViewTest
{
	@Autowired
	private ListView listView;

	@Test
	public void formShownWhenContactSelected() {
		Grid<Contact> grid = this.listView.grid;
		Contact firstContact = getFirstItem(grid);
		ContactForm form = this.listView.form;
		Assertions.assertFalse(form.isVisible());
		grid.asSingleSelect().setValue(firstContact);
		Assertions.assertTrue(form.isVisible());
		Assertions.assertEquals(firstContact.getFirstName(), form.firstName.getValue());
	}

	@SuppressWarnings({"unchecked", "static-method"})
	private Contact getFirstItem(Grid<Contact> grid) {
		return ((ListDataProvider<Contact>)grid.getDataProvider()).getItems().iterator().next();
	}
}

