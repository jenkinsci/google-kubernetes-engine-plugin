# Snapshot Installation Instructions

1. Clone the plugin and enter the directory:

       git clone git@github.com:jenkinsci/google-kubernetes-engine-plugin.git
       cd google-kubernetes-engine-plugin

1. Checkout the branch that you would like to build from:
       
       git checkout <branch name>

1. Build the plugin into a .hpi plugin file:

       mvn hpi:hpi

1. Go to **Manage Jenkins** then **Manage Plugins**.
1. In the Plugin Manager, click the **Advanced** tab and then **Choose File** under the **Upload Plugin** section.
1. Choose the Jenkins plugin file built in Step 3.
1. Click the **Upload** button.