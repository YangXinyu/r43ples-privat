<#include "superHeader.ftl">

<script type="text/javascript">
  
</script>
<style>
   fieldset {
      border : solid 3px black;
   }
</style>

<div class="container">
			<div class="row" >
				<div class="small-6 columns push-3">
                <div class="panel radius" style="background-color:white;">
                  <fieldset style = "border: solid black 5px">
                    <legend><h4><strong>Rebase Unfriendly</strong></h4></legend>
                    </br>
                     <fieldset style="background-color:white">
                      <legend><h6><strong>Rebase Unfriendly</strong></h6></legend>
                      <div class="panel-body">
                        <p>Rebase process stop, because it is rebase unfriendly!</p>
                        <p>Please select: Force Rabase or Manually operation! </p>
                        <p>Force Rebase is dangers and suggest manually operation!</p>

                      </div>



                    </fieldset>
                    </br>

                    <div class="row">
                      <div class="small-6 columns">
                       <a href="manualRebaseProcess?graph=${graphName}&client=${clientName}"><button type="submit" class="button tiny expand default">Manually Rebase</button></a>
                      </div>
                      <div class="small-6 columns">
                        <a href="forceRebaseProcess?graph=${graphName}&client=${clientName}"><button type="button" class="button tiny expand alert ">Force Rebase</button>
                      </div></a>
                    </div>
                    <div class="row">
                      <div class="small-12 columns" style="margin-top:6px">
                        <a href ="merging"><button type="button" class="button tiny  expand success">Restart and go back</button></a>
                      </div>
                    </div>
                        
                  </fieldset>
                </div>
			  </div>
  	  </div>
</div>
   
   
<#include "superFooter.ftl">
	
		
