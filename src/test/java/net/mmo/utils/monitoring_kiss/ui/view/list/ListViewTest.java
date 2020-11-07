/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 17.10.2020
 */

package net.mmo.utils.monitoring_kiss.ui.view.list;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ListDataProvider;
import net.mmo.utils.monitoring_kiss.backend.entity.Contact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// The @RunWith(SpringRunner.class) and @SpringBootTest annotations make sure that the Spring Boot 
// application is initialized before the tests are run and allow you to use @Autowired in the test.
@SpringBootTest // makes sure that the Spring Boot application is initialized before the tests are run and allows you to use @Autowire in the test.
public class ListViewTest
{
	@Autowired
	private ListView listView;

	@Test
	public void formShownWhenContactSelected() {
		Grid<Contact> grid = listView.grid;
		Contact firstContact = getFirstItem(grid);
		ContactForm form = listView.form;
		Assertions.assertFalse(form.isVisible());
		grid.asSingleSelect().setValue(firstContact);
		Assertions.assertTrue(form.isVisible());
		Assertions.assertEquals(firstContact.getFirstName(), form.firstName.getValue());
	}

	@SuppressWarnings("unchecked")
	private Contact getFirstItem(Grid<Contact> grid) {
		return ((ListDataProvider<Contact>)grid.getDataProvider()).getItems().iterator().next();
	}
}

