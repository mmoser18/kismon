/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.connections.JDBCConnection;

@Slf4j
public class JDBCConnectionForm extends IPConnectionForm<JDBCConnection>
{
	private static final long serialVersionUID = 5726368043436831511L;

	static final String UidClassName   = NodeFormField + "dbUid"; //$NON-NLS-1$
	static final String PwdClassName   = NodeFormField + "dbPwd"; //$NON-NLS-1$
	static final String QueryClassName = NodeFormField + "query"; //$NON-NLS-1$
	static final String ResultingQueryClassName = NodeFormField + "resulting-query"; //$NON-NLS-1$
	static final String DriverNameClassName = NodeFormField + "drivername"; //$NON-NLS-1$
	static final String DbResponseClassName = NodeFormField + "db-response"; //$NON-NLS-1$

	protected TextField dbUid;
	protected TextField dbPwd;

	// original query (possibly containing properties):
	protected TextField query;
	// resolved query - displayed only when different from above
	protected TextField resultingQuery;

	protected TextField driverName;

	// response values
	protected TextField responseStatus;
	protected TextArea  responsePayload;


	public JDBCConnectionForm() {
		super(JDBCConnection.class);
	}

	@Override
	protected void createFormFields() {
		super.createFormFields();

		this.dbUid = new TextField(Messages.getString("JDBCConnectionForm.DbUid.Label")); //$NON-NLS-1$
		this.dbUid.setClassName(UidClassName);
		this.dbUid.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.dbUid).withValidator(this.propertiesResolvable);

		this.dbPwd = new TextField(Messages.getString("JDBCConnectionForm.DbPwd.Label")); //$NON-NLS-1$
		this.dbPwd.setClassName(PwdClassName);
		this.dbPwd.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.dbPwd).withValidator(this.propertiesResolvable);

		HorizontalLayout uidAndPwd = new HorizontalLayout(this.dbUid, this.dbPwd);
		uidAndPwd.addClassName(CombinedClassName);

		this.query = new TextField(Messages.getString("JDBCConnectionForm.Query.Label")); //$NON-NLS-1$
		this.query.setClassName(QueryClassName);
		this.query.setThemeName(LabelPaddingTheme);
		this.query.addValueChangeListener(event ->
			{
				log.info("query value changed from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					try {
						updateResultingQuery(event.getValue());
					} catch (Exception ex) {
						log.error("Exception updating query", ex); //$NON-NLS-1$
					}
				}
			});
		this.binder.forField(this.query).withValidator(this.propertiesResolvable);

		this.resultingQuery = new TextField(Messages.getString("JDBCConnectionForm.ResultingQuery.Value")); //$NON-NLS-1$
		this.resultingQuery.setClassName(ResultingQueryClassName);
		this.resultingQuery.setThemeName(LabelPaddingTheme);
		this.resultingQuery.setVisible(false);
		this.binder.forField(this.resultingQuery).bind(JDBCConnection::getResultingQuery, null);

		this.driverName = new TextField(Messages.getString("JDBCConnectionForm.DriverName.Label")); //$NON-NLS-1$
		this.driverName.setClassName(DriverNameClassName);
		this.driverName.setThemeName(LabelPaddingTheme);
		this.binder.forField(this.driverName).withValidator(this.propertiesResolvable);

		this.responseStatus = new TextField(Messages.getString("JDBCConnectionForm.ResponseStatus.Label")); //$NON-NLS-1$
		this.responseStatus.setClassName(DbResponseClassName);
		this.responseStatus.setThemeName(LabelPaddingTheme);
		this.responseStatus.addThemeName(MonospaceTheme);
		this.binder.forField(this.responseStatus).bind(JDBCConnection::getResponseStatus, null);

		HorizontalLayout responseResults = new HorizontalLayout(this.responseStatus, this.duration, this.timestamp);
		responseResults.addClassName(CombinedClassName);

		this.responsePayload = new TextArea(Messages.getString("JDBCConnectionForm.ResponsePayload.Label")); //$NON-NLS-1$
		this.responsePayload.setClassName(DbResponseClassName);
		this.responsePayload.setThemeName(LabelPaddingTheme);
		this.responsePayload.addThemeName(MonospaceTheme);
		this.binder.forField(this.responsePayload).bind(JDBCConnection::getResponsePayload, null);

		this.fields.add(uidAndPwd
		                ,this.query
		                ,this.resultingQuery
		                ,this.execute
		                ,this.driverName
		                ,new Label(Messages.getString("HTTPConnectionForm.Result.Label")) //$NON-NLS-1$
		                ,responseResults
		                ,this.responsePayload
		                ,this.checkResults
		                // ,this.resultChecker
		            	,this.condition
		            	,this.operand
		                );
	}

	@Override
	protected void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingQuery(this.node != null ? this.node.getQuery() : null);
	}

	protected void updateResultingQuery(String newValue) {
		updateResultingField("updateResultingQuery", //$NON-NLS-1$
		                     this.node,
		                     newValue,
		                     JDBCConnection::setResultingQuery,
		                     this.resultingQuery
		                    );
	}
}
