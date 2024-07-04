package com.soa.bonitasoft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.engine.api.APIClient;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.session.APISession;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Servlet WebhookConekta
 * 
 * This servlet does not check any access. All security access must be done before this Servlet
 *
 */
public class ServletWebhookConekta extends HttpServlet{ 
    /**
     * 
     */
    private static final long serialVersionUID = 6980902947325600709L;

    public Logger logger = Logger.getLogger(ServletWebhookConekta.class.getName());

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
    }

    /**
     * Each URL come
     */
    @Override
    public void doPost(HttpServletRequest httpRequest, HttpServletResponse resp) throws ServletException,  IOException {
        Connection CON = null;
        ResultSet RS = null;
        PreparedStatement PSTM = null;
        String USERNAME = "";
        String PASSWORD = "";
        StringBuilder jb = new StringBuilder();
        String line = null;
        String tipoPago = null;
        String email = null;
        String id = null;
        String caseId = null;
        String consulta = null;
        JSONParser parser = new JSONParser();
        JSONObject objResponseConekta = new JSONObject();
        JSONObject dataJson = new JSONObject();
        JSONObject objectJson = new JSONObject();
        JSONObject customerInfoJson = new JSONObject();
        BufferedReader reader = null;
        APISession session = null;
        Boolean isPagoValidadoCont = false;
        ProcessAPI processAPI = null;
        Map<String, Serializable> inputs = null;
        List<HumanTaskInstance> lstHumanTaskInstances = new ArrayList<>();
        
        try {
            /*======================================================================*/
            /*VALIDAR IP DE LA SOLICITUD*/
            /*======================================================================*/
            
            reader = httpRequest.getReader();
            while ((line = reader.readLine()) != null) {
                jb.append(line);
            }

            objResponseConekta = (JSONObject) parser.parse(jb.toString());
            tipoPago = (String) objResponseConekta.get("type");
            
            CON = new DBConnect().getConnection();
            
            if(tipoPago != null && tipoPago.equals("order.paid")) {
                dataJson = (JSONObject) objResponseConekta.get("data");
                objectJson = (JSONObject) dataJson.get("object");
                customerInfoJson = (JSONObject) objectJson.get("customer_info");
                email = (String) customerInfoJson.get("email");

                /*======================================================================*/
                /*CONEXION A BASE DE DATOS*/
                /*======================================================================*/
                consulta = "SELECT correoelectronico, caseid FROM solicituddeadmision WHERE correoelectronico = ?";
                PSTM = CON.prepareStatement(consulta);
                PSTM.setString(1, email);
                RS = PSTM.executeQuery();

                if(RS.next()) {
                    caseId=RS.getString("caseid");
                }
                
                /*======================================================================*/
                /*CONEXION A SERVIDOR BONITA*/
                /*======================================================================*/
                if(caseId != null) {
                    PSTM.close();
                    PSTM = null;
                    RS.close();
                    RS = null;
                    consulta="SELECT persistenceid, persistenceversion, clave, valor, descripcion FROM CatConfiguracion WHERE clave IN ('filterUsername', 'filterPassword')";                   
                    PSTM = CON.prepareStatement(consulta);
                    RS = PSTM.executeQuery();

                    while(RS.next()) {
                        if(RS.getString("clave").equals("filterUsername")){
                            USERNAME = RS.getString("valor");
                        }
                        if(RS.getString("clave").equals("filterPassword")){
                            PASSWORD = RS.getString("valor");
                        }
                    }
                    
                    // First of all, let's log in on the engine:
                    org.bonitasoft.engine.api.APIClient apiClient = new APIClient();
                    apiClient.login(USERNAME, PASSWORD);
                    processAPI = apiClient.getProcessAPI();
                    session = apiClient.getSession();
                    
                    lstHumanTaskInstances = processAPI.getHumanTaskInstances(Long.valueOf(caseId), "Esperar pago", 0, 99);
                    if(lstHumanTaskInstances != null && lstHumanTaskInstances.size()>0) {
                        isPagoValidadoCont = true;
                        inputs = new HashMap<String, Serializable>();
                        inputs.put("isPagoValidadoCont",  isPagoValidadoCont);
                        inputs.put("isCambioMetodoPagoCont",  false);
                        processAPI.assignUserTask(lstHumanTaskInstances.get(0).getId(), session.getUserId());
                        processAPI.executeUserTask(lstHumanTaskInstances.get(0).getId(), inputs);
                    } else {
						/*======================================================================*/
						/*En caso de no encontrar una tarea buscamos por orden en apoyoeducativo */
						/*CONEXION A BASE DE DATOS */
						/*======================================================================*/
						PSTM.close();
						PSTM = null;
						RS.close();
						RS = null;
						consulta = "SELECT correoelectronico, sdae.caseid  FROM solicituddeadmision  AS admi INNER JOIN SolicitudApoyoEducativo AS sdae ON sdae.caseIdAdmisiones = admi.caseid WHERE ordenpagoconekta = ?";
						PSTM = CON.prepareStatement(consulta);
						String ordenPago = (String) objectJson.get("id");
						PSTM.setString(1, ordenPago);
						RS = PSTM.executeQuery();
	
						if(RS.next()) {
							caseId=RS.getString("caseid");
						}
	
						if(caseId != null) {
							lstHumanTaskInstances = processAPI.getHumanTaskInstances(Long.valueOf(caseId), "Esperar pago de estudio socio-económico", 0, 99);
							if(lstHumanTaskInstances != null && lstHumanTaskInstances.size()>0) {
								isPagoValidadoCont = true;
								inputs = new HashMap<String, Serializable>();
								inputs.put("isPagoValidadoInput",  true);
                                inputs.put("isPagoRechazadoInput",  false);
								processAPI.assignUserTask(lstHumanTaskInstances.get(0).getId(), session.getUserId());
								processAPI.executeUserTask(lstHumanTaskInstances.get(0).getId(), inputs);
							}
						}
					}
                }
            } else {
                if(tipoPago != null) {
                    if( !tipoPago.equals("order.created") && !tipoPago.equals("order.pending_payment") && !tipoPago.equals("webhook_ping") ) {
                        dataJson = (JSONObject) objResponseConekta.get("data");
                        objectJson = (JSONObject) dataJson.get("object");
                        id = (String) objectJson.get("id");
                        
                        /*======================================================================*/
                        /*CONEXION A BASE DE DATOS*/
                        /*======================================================================*/      
                        consulta = "SELECT ordenPago, caseid FROM detallesolicitud WHERE ordenPago = ?";
                        PSTM = CON.prepareStatement(consulta);
                        PSTM.setString(1, id);
                        RS = PSTM.executeQuery();

                        if(RS.next()) {
                            caseId=RS.getString("caseid");
                        }
                        
                        /*======================================================================*/
                        /*CONEXION A SERVIDOR BONITA*/
                        /*======================================================================*/
                        if(caseId != null) {
                            PSTM.close();
                            PSTM = null;
                            RS.close();
                            RS = null;
                            consulta = "SELECT persistenceid, persistenceversion, clave, valor, descripcion FROM CatConfiguracion WHERE clave IN ('filterUsername', 'filterPassword')";                   
                            PSTM = CON.prepareStatement(consulta);
                            RS = PSTM.executeQuery();

                            while(RS.next()) {
                                if(RS.getString("clave").equals("filterUsername")){
                                    USERNAME = RS.getString("valor");
                                }
                                if(RS.getString("clave").equals("filterPassword")){
                                    PASSWORD = RS.getString("valor");
                                }
                            }
                                
                            // First of all, let's log in on the engine:
                            org.bonitasoft.engine.api.APIClient apiClient = new APIClient();
                            apiClient.login(USERNAME, PASSWORD);
                            processAPI = apiClient.getProcessAPI();
                            session = apiClient.getSession();
                            lstHumanTaskInstances = processAPI.getHumanTaskInstances(Long.valueOf(caseId), "Esperar pago", 0, 99);
                            
                            if(lstHumanTaskInstances != null && lstHumanTaskInstances.size()>0) {
                                inputs = new HashMap<String, Serializable>();
                                inputs.put("isPagoValidadoCont",  isPagoValidadoCont);
                                inputs.put("isCambioMetodoPagoCont",  false);
                                processAPI.assignUserTask(lstHumanTaskInstances.get(0).getId(), session.getUserId());
                                processAPI.executeUserTask(lstHumanTaskInstances.get(0).getId(), inputs);
                            } else {
                                /*======================================================================*/
                                /*En caso de no encontrar una tarea buscamos por orden en apoyoeducativo */
                                /*CONEXION A BASE DE DATOS */
                                /*======================================================================*/
                                
								PSTM.close();
								PSTM = null;
								RS.close();
								RS = null;
								
								consulta = "SELECT correoelectronico, sdae.caseid  FROM solicituddeadmision  AS admi INNER JOIN SolicitudApoyoEducativo AS sdae ON sdae.caseIdAdmisiones = admi.caseid WHERE ordenpagoconekta = ?";
								
								PSTM = CON.prepareStatement(consulta);
								String ordenPago = (String) objectJson.get("id");
								PSTM.setString(1, ordenPago);
								RS = PSTM.executeQuery();
			
								if(RS.next()) {
									caseId = RS.getString("caseid");
								}
			
								if(caseId != null) {
									lstHumanTaskInstances = processAPI.getHumanTaskInstances(Long.valueOf(caseId), "Esperar pago de estudio socio-económico", 0, 99);
									if(lstHumanTaskInstances != null && lstHumanTaskInstances.size( ) >0) {
										isPagoValidadoCont = true;
										inputs = new HashMap<String, Serializable>();
										inputs.put("isPagoValidadoInput",  true);
		                                inputs.put("isPagoRechazadoInput",  false);
										processAPI.assignUserTask(lstHumanTaskInstances.get(0).getId(), session.getUserId());
										processAPI.executeUserTask(lstHumanTaskInstances.get(0).getId(), inputs);
									}
								}
							}
                        }
                    }
                }
            }

            resp.getWriter().write("OK");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("FilterWebhookConekta: exception "+e.getMessage()+" at "+exceptionDetails);
            throw new ServletException("FilterWebhookConekta: ServletException "+e.getMessage()+" at "+exceptionDetails);
        } finally {
            new DBConnect().closeObj(CON, RS, PSTM);
        }

        return;
    }

    @Override
    public void destroy() {

    }
}