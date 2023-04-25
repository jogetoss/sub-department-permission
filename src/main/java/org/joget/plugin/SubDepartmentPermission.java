/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.joget.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DatalistPermission;
import org.joget.apps.form.model.FormPermission;
import org.joget.apps.userview.model.UserviewPermission;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.directory.model.Department;
import org.joget.directory.model.Employment;
import org.joget.directory.model.Organization;
import org.joget.directory.model.User;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author User
 */
public class SubDepartmentPermission extends UserviewPermission implements PluginWebSupport, FormPermission, DatalistPermission  {
    
    @Override
    public boolean isAuthorize() {
        User user = getCurrentUser();

        if (user != null && user.getEmployments() != null && user.getEmployments().size() > 0) {
            Set<String> allowedDeptIds = new HashSet<String>(Arrays.asList(getPropertyString("allowedDeptIds").split(";")));
            //check user deptId = department selected in property options
            for (Employment e : (Set<Employment>) user.getEmployments()) {
                if (e.getDepartmentId() != null && allowedDeptIds.contains(e.getDepartmentId())) {
                    return true;
                }                
            }
            
            //check 
            String[] allowedDeptList = getPropertyString("allowedDeptIds").split(";");
            for (String deptId: allowedDeptList) { 
                System.out.println("deptId=========> "+deptId); 
                                
                ApplicationContext ac = AppUtil.getApplicationContext();
                ExtDirectoryManager directoryManager = (ExtDirectoryManager) ac.getBean("directoryManager");
                //retrieve all subdepartments based on deptId
                Collection<Department> childDeptList = directoryManager.getDepartmentsByParentId(null, deptId, "name", false, null, null);
                             
                //if there are sub departments, authorize
                if(!childDeptList.isEmpty()){
                        return true;                    
                }
                
            }
            
        }

        return false;
    }

    public String getName() {
        return "Department and Sub Department Permission";
    }
    
    public String getDescription() {
        return "Checks if the user currently logged in belongs to the selected department or any sub departments under the selected department before letting the user view the screen. ";
    }

    public String getVersion() {
        return "7.0.1";
    }

    public String getLabel() {
        return "Department and Sub Department";
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/subdepartmentPermission.json", null, true, "messages/subdepartmentPermission");
    }

    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        boolean isAdmin = WorkflowUtil.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN);
        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        String action = request.getParameter("action");

        if ("getOrgs".equals(action)) {
            try {
                JSONArray jsonArray = new JSONArray();

                ApplicationContext ac = AppUtil.getApplicationContext();
                ExtDirectoryManager directoryManager = (ExtDirectoryManager) ac.getBean("directoryManager");

                Map<String, String> empty = new HashMap<String, String>();
                empty.put("value", "");
                empty.put("label", ResourceBundleUtil.getMessage("console.directory.user.empty.option.label"));
                jsonArray.put(empty);
                
                Collection<Organization> orgList = directoryManager.getOrganizationsByFilter(null, "name", false, null, null);

                for (Organization o : orgList) {
                    Map<String, String> option = new HashMap<String, String>();
                    option.put("value", o.getId());
                    option.put("label", o.getName());
                    jsonArray.put(option);
                }

                jsonArray.write(response.getWriter());
            } catch (Exception ex) {
                LogUtil.error(this.getClass().getName(), ex, "Get Organization options Error!");
            }
        } else if ("getDepts".equals(action)) {
            String orgId = request.getParameter("orgId");

            if ("null".equals(orgId) || "".equals(orgId)) {
                orgId = null;
            }

            try {
                JSONArray jsonArray = new JSONArray();

                ApplicationContext ac = AppUtil.getApplicationContext();
                ExtDirectoryManager directoryManager = (ExtDirectoryManager) ac.getBean("directoryManager");

                Collection<Department> deptList = directoryManager.getDepartmentsByOrganizationId(null, orgId, "name", false, null, null);

                for (Department d : deptList) {
                    Map<String, String> option = new HashMap<String, String>();
                    option.put("value", d.getId());
                    option.put("label", ((d.getTreeStructure() != null) ? d.getTreeStructure() + " " : "") + d.getName());
                    jsonArray.put(option);
                }

                jsonArray.write(response.getWriter());
            } catch (Exception ex) {
                LogUtil.error(this.getClass().getName(), ex, "Get departments options Error!");
            }
        
        } else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
}
