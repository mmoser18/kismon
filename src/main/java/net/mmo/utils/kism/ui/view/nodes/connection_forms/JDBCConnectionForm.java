/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import net.mmo.utils.kism.entities.connections.JDBCConnection;
import net.mmo.utils.kism.entities.connections.RESTConnection;

public class JDBCConnectionForm extends ConnectionForm<JDBCConnection>
{
	private static final long serialVersionUID = 5726368043436831511L;

	public static final String UidClassName   = "node-form-field-uid"; //$NON-NLS-1$
	public static final String PwdClassName   = "node-form-field-pwd"; //$NON-NLS-1$
	public static final String QueryClassName = "node-form-field-query"; //$NON-NLS-1$
	public static final String ResultingQueryClassName = "node-form-field-resulting-query"; //$NON-NLS-1$
	public static final String DriverNameClassName = "node-form-field-drivername"; //$NON-NLS-1$
	public static final String DbResponseClassName = "node-form-field-db-response"; //$NON-NLS-1$

	protected TextField uid;
	protected TextField pwd;

	protected TextField query;
	protected TextField resultingQuery;

	protected TextField driverName;

	protected TextArea dbResponse;


	public JDBCConnectionForm() {
		super(JDBCConnection.class);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.uid = new TextField(Messages.getString("JDBCConnectionForm.Uid.Label")); //$NON-NLS-1$
		this.uid.setClassName(UidClassName);
		this.uid.setThemeName(LabelPaddingTheme);

		this.pwd = new TextField(Messages.getString("JDBCConnectionForm.Pwd.Label")); //$NON-NLS-1$
		this.pwd.setClassName(PwdClassName);
		this.pwd.setThemeName(LabelPaddingTheme);

		HorizontalLayout uidAndPwd = new HorizontalLayout(this.uid, this.pwd);
		uidAndPwd.addClassName(CombinedClassName);

		this.query = new TextField(Messages.getString("JDBCConnectionForm.Query.Label")); //$NON-NLS-1$
		this.query.setClassName(QueryClassName);
		this.query.setThemeName(LabelPaddingTheme);
		this.query.addValueChangeListener(event -> { if (event.isFromClient()) updateResultingQuery(); });

		this.resultingQuery = new TextField("-->"); //$NON-NLS-1$
		this.resultingQuery.setClassName(ResultingQueryClassName);
		this.binder.forField(this.resultingQuery).bind(JDBCConnection::getResultingQuery, null);

		this.driverName = new TextField(Messages.getString("JDBCConnectionForm.DriverName.Label")); //$NON-NLS-1$
		this.driverName.setClassName(DriverNameClassName);
		this.driverName.setThemeName(LabelPaddingTheme);

		this.dbResponse = new TextArea(Messages.getString("JDBCConnectionForm.DbResponse.Label")); //$NON-NLS-1$
		this.dbResponse.setClassName(DbResponseClassName);
		this.dbResponse.setClassName(MonospaceClassName);
		this.dbResponse.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.dbResponse).bind(JDBCConnection::getDbResponse, null);

		this.fields.add(uidAndPwd,
		                this.query,
		                this.resultingQuery,
		                this.execute,
		                this.driverName,
		                new Label(Messages.getString("HTTPConnectionForm.Result.Label")), //$NON-NLS-1$
		                this.duration,
		                this.dbResponse);
	}

	protected void updateResultingQuery() {
		this.resultingQuery.setValue(String.format(Messages.getString("JDBCConnectionForm.ResultingQuery.Value"),this.node.getQuery())); //$NON-NLS-1$
		// TODO: to be implemented
	}

	@Override
	protected void handleChangeEvent(JDBCConnection sourceNode, String propertyName, Object oldValue, Object newValue) {
		switch (propertyName) {
		case JDBCConnection.PROPERTYNAME_DB_RESPONSE:
			if (individualUpdates) {
				this.dbResponse.setValue((String)newValue);
			}
			break;
		case RESTConnection.PROPERTYNAME_RESPONSE_COMPLETE:
			if (!individualUpdates) { // do all of the above in one go:
				this.dbResponse.setValue(sourceNode.getDbResponse());
			}
			break;
		default:
			super.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
		}
	}

}
