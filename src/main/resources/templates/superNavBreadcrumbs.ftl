<!-- Navigation -->
  <div class="fixed">
    <nav class = "top-bar" data-topbar role = "navigation">
      <ul class = "title-area">
        <li class = "name">
          <h1><a href="."><img class="pull-left" src="/static/images/r43ples-r-logo.v04.png" alt="R43ples logo" style="height:100%;width:32px"><span>R43ples</span></a></h1>
        </li>
        <li class="toggle-topbar menu-icon"><a href="#"><span>Menu</span></a></li>
      </ul>
      <section class="top-bar-section">

          <!-- Right Nav Section -->
          <ul class="left">
            <#if endpoint_active??>
              <li class="active" ><a href="sparql"><i class="fa fa-edit"></i> Endpoint</a></li>
            <#else>
              <li ><a href="sparql"><i class="fa fa-edit"></i> Endpoint</a></li>
            </#if>

            <#if debug_active?? >
              <li class="active"><a href="debug"><i class="fa fa-stethoscope"></i> Debug</a></li>
            <#else>
              <li ><a href="debug"><i class="fa fa-stethoscope"></i> Debug</a></li>
            </#if>

            <#if merging_active??>
              <li class="active"><a href="merging"><i class="fa fa-magic"></i> Merging</a></li>
            <#else>
              <li ><a href="merging"><i class="fa fa-magic"></i> Merging</a></li>
            </#if>

            <#if mergingConfiguration_active??>
              <li class="active"><a href="mergingConfiguration"><i class="fa fa-cog fa-fw"></i> Configuration</a></li>
            <#else>
              <li ><a href="mergingConfiguration"><i class="fa fa-cog fa-fw"></i> Configuration</a></li>
            </#if>

            <li class="divider"></li>
          </ul>

          <!-- Left Nav Section -->
          <ul class="right">
            <li><div style="margin:6px; padding:3px;" ><em style="margin:16px; padding:8px; color:white;"> <#if version??>Version: ${version!"  "} 
              <#else>Git: ${gitCommit!" "} - ${gitBranch!" "}</#if> </em></div></li>
            <li class="divider"></li>
            <li><a href="http://plt-tud.github.io/r43ples/"><i class="fa fa-globe"></i> Website</a></li>
            <li><a href="https://github.com/plt-tud/r43ples"><i class="fa fa-github"></i> GitHub</a></li>
          </ul>
        </section>
      </nav>
      
        <ul class="breadcrumbs  columns small-12 ">
          <li style = "margin-left:0rem;">
              <#if isRebase>
                <a href ="rebaseReportProcess?graph=${graphName}&client=${clientName}">Report</a>
              <#else>
                <a href ="reportProcess?graph=${graphName}&client=${clientName}">Report</a>
              </#if>                       
          </li>

          <li class = "current" style = "margin-left:0rem;"><a href="#" id="triple">Triple view</a></li>
          <li><a href="#" id="individual" >Individual</a></li>
          <li><a href="#" id="highLevel" >High level</a></li>  
       
        </ul>
  </div>