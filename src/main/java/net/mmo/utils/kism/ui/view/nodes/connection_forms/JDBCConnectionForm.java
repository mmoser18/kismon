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

public class JDBCConnectionForm extends IPConnectionForm<JDBCConnection>
{
	private static final long serialVersionUID = 5726368043436831511L;

	static final String UidClassName   = "node-form-field-uid"; //$NON-NLS-1$
	static final String PwdClassName   = "node-form-field-pwd"; //$NON-NLS-1$
	static final String QueryClassName = "node-form-field-query"; //$NON-NLS-1$
	static final String ResultingQueryClassName = "node-form-field-resulting-query"; //$NON-NLS-1$
	static final String DriverNameClassName = "node-form-field-drivername"; //$NON-NLS-1$
	static final String DbResponseClassName = "node-form-field-db-response"; //$NON-NLS-1$

	protected TextField uid;
	protected TextField pwd;

	protected TextField query;
	protected TextField resultingQuery;

	protected TextField driverName;

	protected TextField responseStatus;
	protected TextArea  responsePayload;


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
		this.query.addValueChangeListener(event ->
			{
				if (event.isFromClient()) updateResultingQuery(event.getValue());
			});

		this.resultingQuery = new TextField(Messages.getString("JDBCConnectionForm.ResultingQuery.Value")); //$NON-NLS-1$
		this.resultingQuery.setClassName(ResultingQueryClassName);
		this.resultingQuery.setThemeName(LabelPaddingTheme);
		this.resultingQuery.setVisible(false);
		this.binder.forField(this.resultingURL).bind(JDBCConnection::getResultingQuery, null);

		this.driverName = new TextField(Messages.getString("JDBCConnectionForm.DriverName.Label")); //$NON-NLS-1$
		this.driverName.setClassName(DriverNameClassName);
		this.driverName.setThemeName(LabelPaddingTheme);

		this.responseStatus = new TextField(Messages.getString("JDBCConnectionForm.ResponseStatus.Label")); //$NON-NLS-1$
		this.responseStatus.setClassName(DbResponseClassName);
		this.responseStatus.setThemeName(LabelPaddingTheme);
		this.responseStatus.addThemeName(MonospaceTheme);
		this.binder.forField(this.responseStatus).bind(JDBCConnection::getResponseStatus, null);

		HorizontalLayout responseStatusAndDuration = new HorizontalLayout(this.responseStatus, this.duration);
		responseStatusAndDuration.addClassName(CombinedClassName);

		this.responsePayload = new TextArea(Messages.getString("JDBCConnectionForm.ResponsePayload.Label")); //$NON-NLS-1$
		this.responsePayload.setClassName(DbResponseClassName);
		this.responsePayload.setThemeName(LabelPaddingTheme);
		this.responsePayload.addThemeName(MonospaceTheme);
		this.binder.forField(this.responsePayload).bind(JDBCConnection::getResponsePayload, null);

		this.fields.add(uidAndPwd,
		                this.query,
		                this.resultingQuery,
		                this.execute,
		                this.driverName,
		                new Label(Messages.getString("HTTPConnectionForm.Result.Label")), //$NON-NLS-1$
		                responseStatusAndDuration,
		                this.responsePayload);
	}

	protected void updateResultingQuery(String newValue) {
		updateResultingField("updateResultingQuery", //$NON-NLS-1$
		                     this.node,
		                     newValue,
		                     JDBCConnection::setResultingQuery,
		                     this.resultingQuery
		                    );
	}

	@Override
	protected void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingQuery(this.node.getQuery());
	}

	@Override
	protected void handleChangeEvent(JDBCConnection sourceNode, String propertyName, Object oldValue, Object newValue) {
		switch (propertyName) {
		case JDBCConnection.PROPERTYNAME_RESPONSE_STATUS:
			if (individualUpdates) {
				this.responseStatus.setValue((String)newValue);
			}
			break;
		case JDBCConnection.PROPERTYNAME_RESPONSE_PAYLOAD:
			if (individualUpdates) {
				this.responsePayload.setValue((String)newValue);
			}
			break;
		case RESTConnection.PROPERTYNAME_RESPONSE_COMPLETE:
			if (!individualUpdates) { // do all of the above in one go:
				this.responseStatus.setValue(sourceNode.getResponseStatus());
				this.responsePayload.setValue(sourceNode.getResponsePayload());
			}
			break;
		default:
			super.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
		}
	}

}
