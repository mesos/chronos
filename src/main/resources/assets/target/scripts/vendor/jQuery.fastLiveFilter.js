/**
 * fastLiveFilter jQuery plugin 1.0.3
 * 
 * Copyright (c) 2011, Anthony Bush
 * License: <http://www.opensource.org/licenses/bsd-license.php>
 * Project Website: http://anthonybush.com/projects/jquery_fast_live_filter/
 **/

jQuery.fn.fastLiveFilter=function(e,t){t=t||{},e=jQuery(e);var n=this,r=t.timeout||0,i=t.callback||function(){},s,o=$(".item"),u=o.length,a=u>0?o[0].style.display:"block";return i(u),n.change(function(){var e=n.val().toLowerCase(),t,r=0,s=[];for(var f=0;f<u;f++)t=o[f],(t.textContent||t.innerText||"").toLowerCase().indexOf(e)>=0?(t.style.display=="none"&&(t.style.display=a),s.push(app.jobsCollection.get(t.classList[1])),r++):t.style.display!="none"&&(t.style.display="none");return i(r,s),!1}).keydown(function(){clearTimeout(s),s=setTimeout(function(){n.change()},r)}),this};