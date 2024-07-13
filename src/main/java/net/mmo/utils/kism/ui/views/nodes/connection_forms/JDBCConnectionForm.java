/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.views.nodes.connection_forms;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.MonospaceTheme;
import static net.mmo.utils.kism.ui.UIConstants.NodeFormField;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PreserveOnRefresh;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.connections.JDBCConnection;
import net.mmo.utils.kism.entities.nodes.connections.TCPConnection;
import net.mmo.utils.kism.ui.CommonConstants;

@SuppressWarnings("javadoc")
@PreserveOnRefresh
public class JDBCConnectionForm extends TCPConnectionForm<JDBCConnection>
{
	private static final long serialVersionUID = 5726368043436831511L;

	// these are package visible so that unit-tests can access them:
	static final String UidClassName   = NodeFormField + "dbUid"; //$NON-NLS-1$
	static final String PwdClassName   = NodeFormField + "dbPwd"; //$NON-NLS-1$
	static final String QueryClassName = NodeFormField + "query"; //$NON-NLS-1$
	static final String ResultingQueryClassName = NodeFormField + "resulting-query"; //$NON-NLS-1$
	static final String DriverNameClassName = NodeFormField + "drivername"; //$NON-NLS-1$
	static final String DbResponseClassName = NodeFormField + "db-response"; //$NON-NLS-1$

	protected TextField dbUid;
	protected PasswordField dbPwd;

	// original query (possibly containing properties):
	protected TextField query;
	// resolved query - displayed only when different from above
	protected TextField resultingQuery;

	protected TextField driverName;

	// response values
	protected TextField responseStatus;
	protected TextArea  responsePayload;


	@Override
	public void init(NodeService nodeService) {
		super.init(JDBCConnection.class, nodeService);
	}

	@Override
	protected void createFormFields(NodeService nodeService) {
		super.createFormFields(nodeService);
		// overriding the default URL label:
		this.url.setLabel(Messages.getString("JDBCConnectionForm.URL.Label")); //$NON-NLS-1$

		// had to movethe URL validation here since JDBC-URLs look completely differently and
		// didn't pass the conventional URI check!
		this.binder.forField(this.url)
			// .withValidator(this.urlValidator) // the default validator can't cope with the JDBC-URLs
			.bind(TCPConnection::getUrl, TCPConnection::setUrl);

		this.binder.forField(this.resultingURL)
			// .withValidator(this.urlValidator) // the default validator can't cope with the JDBC-URLs
			.bind(TCPConnection::getResultingUrl, null);

		this.driverName = new TextField(Messages.getString("JDBCConnectionForm.DriverName.Label")); //$NON-NLS-1$
		this.driverName.setClassName(DriverNameClassName);
		this.driverName.setThemeName(LabelPaddingTheme);
		this.driverName.setClearButtonVisible(this.clearButtonsVisible);
		this.driverName.setEnabled(this.isAdminUser);
		this.binder.forField(this.driverName)
			.withValidator(this.propertiesResolvableValidator)
			.bind(JDBCConnection::getDriverName, JDBCConnection::setDriverName);

		this.dbUid = new TextField(Messages.getString("JDBCConnectionForm.DbUid.Label")); //$NON-NLS-1$
		this.dbUid.setClassName(UidClassName);
		this.dbUid.setThemeName(LabelPaddingTheme);
		this.dbUid.setClearButtonVisible(this.clearButtonsVisible);
		this.dbUid.setEnabled(this.isAdminUser);
		this.binder.forField(this.dbUid)
			.withValidator(this.propertiesResolvableValidator)
			.bind(JDBCConnection::getDbUid, JDBCConnection::setDbUid);

		this.dbPwd = new PasswordField(Messages.getString("JDBCConnectionForm.DbPwd.Label")); //$NON-NLS-1$
		this.dbPwd.setClassName(PwdClassName);
		this.dbPwd.setThemeName(LabelPaddingTheme);
		this.dbPwd.setClearButtonVisible(this.clearButtonsVisible);
		this.dbPwd.setEnabled(this.isAdminUser);
		this.dbPwd.setRevealButtonVisible(this.isAdminUser);
		if (this.isAdminUser) {
			this.binder.forField(this.dbPwd)
				.withValidator(this.propertiesResolvableValidator)
				.bind(JDBCConnection::getDbPwd, JDBCConnection::setDbPwd);
		} else { // we don't want to send the pwd to the client - not even hidden - if (s)he is not an admin user!
			this.binder.forField(this.dbPwd)
				.bind(CommonConstants::suppressedPassword, null);
		}

		this.query = new TextField(Messages.getString("JDBCConnectionForm.Query.Label")); //$NON-NLS-1$
		this.query.setClassName(QueryClassName);
		this.query.setThemeName(LabelPaddingTheme);
		this.query.setClearButtonVisible(this.clearButtonsVisible);
		this.query.setEnabled(this.isAdminUser);
		this.query.addValueChangeListener(event ->
			{
				this.log.debug("query value changed from '{}' to '{}' (fromClient:{})", event.getOldValue(), event.getValue(), event.isFromClient()); //$NON-NLS-1$
				if (event.isFromClient()) {
					try {
						updateResultingQuery(event.getValue());
					} catch (Exception ex) {
						this.log.error("Exception updating query", ex); //$NON-NLS-1$
					}
				}
			});
		this.binder.forField(this.query)
			.withValidator(this.propertiesResolvableValidator)
			.bind(JDBCConnection::getQuery, JDBCConnection::setQuery);

		this.resultingQuery = new TextField(Messages.getString("JDBCConnectionForm.ResultingQuery.Value")); //$NON-NLS-1$
		this.resultingQuery.setClassName(ResultingQueryClassName);
		this.resultingQuery.setThemeName(LabelPaddingTheme);
		this.resultingQuery.setVisible(false);
		this.resultingQuery.setReadOnly(true);
		this.binder.forField(this.resultingQuery)
			.bind(JDBCConnection::getResultingQuery, null);

		this.responseStatus = new TextField(Messages.getString("JDBCConnectionForm.ResponseStatus.Label")); //$NON-NLS-1$
		this.responseStatus.setClassName(DbResponseClassName);
		this.responseStatus.setThemeName(LabelPaddingTheme);
		this.responseStatus.addThemeName(MonospaceTheme);
		this.responseStatus.setReadOnly(true);
		this.binder.forField(this.responseStatus)
			.bind(JDBCConnection::getResponseStatus, null);

		this.responsePayload = new TextArea(Messages.getString("JDBCConnectionForm.ResponsePayload.Label")); //$NON-NLS-1$
		this.responsePayload.setClassName(DbResponseClassName);
		this.responsePayload.setThemeName(LabelPaddingTheme);
		this.responsePayload.addThemeName(MonospaceTheme);
		this.responsePayload.setReadOnly(true);
		this.binder.forField(this.responsePayload)
			.bind(JDBCConnection::getResponsePayload, null);

		// combining and grouping bits and pieces:
		HorizontalLayout uidAndPwd = new HorizontalLayout(this.dbUid, this.dbPwd);
		uidAndPwd.addClassName(CombinedClassName);

		this.connnectionDetailsPanel.add(this.driverName, uidAndPwd, this.query, this.resultingQuery);

		Accordion requestDetails = new Accordion();
		requestDetails.close(); // initially closed
		VerticalLayout requestDetailsPanel = new VerticalLayout();
		requestDetailsPanel.setClassName(RequestDetailsClassName);
		requestDetailsPanel.add(this.responsePayload);
		requestDetails.add(Messages.getString("JDBCConnectionForm.RequestDetails.Label"), requestDetailsPanel); //$NON-NLS-1$

		HorizontalLayout responseResults = new HorizontalLayout(this.responseStatus, this.duration, this.timestamp);
		responseResults.addClassName(CombinedClassName);

		this.fields.add(new NativeLabel(Messages.getString("HTTPConnectionForm.Result.Label")), //$NON-NLS-1$
		                responseResults,
		                requestDetails,
		                this.validationDetails
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
