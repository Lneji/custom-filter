package org.exoplatform.platform.extension.web;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.filter.Filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*
* Filter to return 404 not found if the navigation does not exist
*
* @author <a href="mailto:lneji@exoplatform.com">Lassaâd Neji</a>
*/
public class CustomFilter implements Filter {
    private static final Log LOG = ExoLogger.getLogger(CustomFilter.class);
    private List<String> listAllPortalURLs = new ArrayList<String>();

    /**
     * Returns all the node of the tree
     * @param root the root Node of the tree
     * @return list all node of the tree
     */
    public List<NodeContext> retrieveAllNode(NodeContext<?> root) {
        List<NodeContext> listAllNode = new ArrayList<NodeContext>();
        listAllNode.add(root);
        for (int i=0; i<listAllNode.size();i++) {
            NodeContext tmp = (NodeContext) listAllNode.get(i);
            if (tmp.getNodes()!=null){
                if (tmp.getNodes().size()>0){
                    for (int j=0;j<tmp. getNodes().size();j++) {
                        NodeContext tmp2 = (NodeContext)tmp.getNode(j);
                        if(!listAllNode.contains(tmp2)) {
                            listAllNode.add(tmp2);
                        }
                    }
                }
            }
        }
        return listAllNode;
    }

    public String concat(String parentNodeName, String nodeName) {
        return parentNodeName+"/"+nodeName;
    }

    public String constructNodePath(NodeContext node) {
        String nodePath = node.getName();
        while (node.getParentNode()!=null) {
            NodeContext parent = (NodeContext) node.getParentNode();
            if (!(parent.getName().equals("default"))) {
                nodePath = concat(parent.getName(),nodePath);
            }
            node = (NodeContext) node.getParentNode();
        }
        return nodePath;
    }

     /**
      * This method construct the list of all the navigation of the node in the tree which it s root is passed as parameter
      * @param root the root Node of the tree
      * @param listToTest all navigation of the tree will be added in this list
      */
    public void listAllPortalURL(NodeContext root, String siteOrGroupName, List<String> listToTest) {
        if (root.listIterator().hasNext()) {
            for (int i = 0;i<root.getNodes().size();i++) {
                NodeContext node = (NodeContext) root.getNode(i);
                String nodePath = constructNodePath(node);
                nodePath = siteOrGroupName+"/"+nodePath;
                if (!listToTest.contains(nodePath)) {
                    listToTest.add(nodePath);
                }
                listAllPortalURL(node, siteOrGroupName, listToTest);
            }
        }
    }

    /**
     * Returns true if the passed groupname is one of current user groups
     *
     */
    public boolean isValidGroup(String groupName) {
        String userName = ConversationState.getCurrent().getIdentity().getUserId();
        Set<String> listUserGroup =  ConversationState.getCurrent().getIdentity().getGroups();
        String groupId = groupName.replaceAll(":","/");
        for (String name:listUserGroup){
            if (name.equals(groupId));
            return true;
        }
        return false;
    }

     /**
      * Returns true if the current group navigation is coherent
      *
      */
    public boolean isValidGroupNavigation(ExoContainer container ,NavigationService navigationService, String groupName, String groupNavigation) {
        List<String> listAllGroupNavigation = new ArrayList<String>();
        if (!groupName.isEmpty()){
        try {
                String groupId = groupName.replaceAll(":","/");
                NavigationContext navigationContext;
                navigationContext = navigationService.loadNavigation(SiteKey.group(groupId));
                if (navigationContext!=null){
                    NodeContext<?> rootNode = navigationService.loadNode(NodeModel.SELF_MODEL, navigationContext, Scope.ALL, null);
                    groupId = groupId.replaceAll("/",":");
                    listAllPortalURL(rootNode, groupId, listAllGroupNavigation);
                }
            if (listAllGroupNavigation.contains(groupNavigation)){
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        }
        return false;
    }

    /**
     * Returns true if the sitename passed as parameter is in the list of portal sites
     *
     */
    public boolean isValidSite(String siteName){
        List<String> listAllSiteName = new ArrayList<String>();
        try {
            UserPortalConfigService dataStorage = (UserPortalConfigService) ExoContainerContext.getCurrentContainer()
                    .getComponentInstanceOfType(UserPortalConfigService.class);
            listAllSiteName = dataStorage.getAllPortalNames();
            if (listAllSiteName.contains(siteName)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> retrieveAllSiteName() {
        List<String> listAllSiteName = new ArrayList<String>();
        try {
            UserPortalConfigService dataStorage = (UserPortalConfigService) ExoContainerContext.getCurrentContainer()
                    .getComponentInstanceOfType(UserPortalConfigService.class);
            listAllSiteName = dataStorage.getAllPortalNames();
            List<String> tmp = new ArrayList<String>();
            for (String s:listAllSiteName){
                tmp.add(s+"/");
            }
            listAllSiteName.addAll(tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listAllSiteName;
    }

      /*
      * Returns true if the current site navigation is coherent
      *
      */
    public boolean isValidSiteNavigation(NavigationService navigationService, String siteName, String siteNavigation) {
        UserPortalConfigService dataStorage = (UserPortalConfigService) ExoContainerContext.getCurrentContainer()
                .getComponentInstanceOfType(UserPortalConfigService.class);
        List<String> listAllSitesNavigation = new ArrayList<String>();
        if (!siteName.isEmpty()){
        try {
            NavigationContext navigationContext;
            navigationContext = navigationService.loadNavigation(SiteKey.portal(siteName));
            if (navigationContext!=null){
                NodeContext<?> rootNode = navigationService.loadNode(NodeModel.SELF_MODEL, navigationContext, Scope.ALL, null);
                listAllPortalURL(rootNode, siteName, listAllSitesNavigation);
            }
            listAllSitesNavigation.addAll(retrieveAllSiteName());
            if (listAllSitesNavigation.contains(siteNavigation)){
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        }
        return false;
    }

      /*
      * Returns the list of all current user navigation
      * to update later
      */
    public List<NodeContext> retrieveAllUserNavigation(NavigationService navigationService, String userName) {
        //String userName = ConversationState.getCurrent().getIdentity().getUserId();
        List<NodeContext> listAllUserNavigation = new ArrayList<NodeContext>();
        NavigationContext nav = navigationService.loadNavigation(SiteKey.user(userName));
        if (nav!=null){
            NodeContext<?> rootNode = navigationService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
            for (int i = 0; i < rootNode.getNodes().size(); ++i) {
                NodeContext child = (NodeContext) rootNode.getNode(i);
            }
            listAllUserNavigation.addAll(retrieveAllNode(rootNode));
        }
        return listAllUserNavigation;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String userName = ConversationState.getCurrent().getIdentity().getUserId();
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        StringBuffer currentURI = req.getRequestURL();
        int indexOfPortal = -1;
        if (currentURI.indexOf("/portal")!=-1){
            if (currentURI.length()>currentURI.indexOf("/portal")+9){
            indexOfPortal = currentURI.indexOf("/portal")+8;
            }
        }
        boolean isUserNav = (currentURI.indexOf("/"+userName)!=-1);
        boolean isGroupNav = currentURI.indexOf("/g/")!=-1;
        boolean isValidNav = (indexOfPortal!=-1);
        boolean isWikiPage = ((currentURI.indexOf("wiki/")!=-1));
        if (isWikiPage || isUserNav) {
            currentURI.setLength(0);
        }
        if (isValidNav){
        try {
            ExoContainer container = ExoContainerContext.getCurrentContainer();
            NavigationService navigationService = (NavigationService) container.getComponentInstance(NavigationService.class);
            RequestLifeCycle.begin(container);
            if (!isGroupNav) {
                String pageNav = "" ;
                if (currentURI.length()>currentURI.indexOf("/portal")+9){
                pageNav = currentURI.substring(indexOfPortal);
                }
                String siteName = "";
                if (pageNav.indexOf("/")!=-1) {
                siteName = pageNav.substring(0,pageNav.indexOf("/"));
                }
                if (isValidSite(siteName)){
                    if (!isValidSiteNavigation(navigationService,siteName,pageNav)){
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                }
            }   else {
                int i = currentURI.indexOf("/portal");
                currentURI = currentURI.replace(i+7,i+9,"");
                String pageNav = currentURI.substring(indexOfPortal);
                String groupName = "";
                if (pageNav.indexOf("/")!=-1){
                    groupName = pageNav.substring(0,pageNav.indexOf("/"));
                }
                if (isValidGroup(groupName)){
                    if (!isValidGroupNavigation(container,navigationService,groupName,pageNav)){
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RequestLifeCycle.end();
        }
    }
        chain.doFilter(request, response);
    }
}

