
// Loads workflow from backend
function loadWorkflow() {
  $.get(apiUrl, function(data) {
    deserializeWorkflow($(data));
  })
  .fail(function(msg) {
    alert( "Error loading workflow from backend: " + msg);
  })
}

function deserializeWorkflow(xml) {
  // Retrieve the xml root element
  var xmlRoot = xml.children('Workflow');
  // Find the editor contents to put the operators into
  var editorContent = $("#editorContent");
  // Remember generated endpoints
  var sourceEndpoints = {};
  var targetEndpoints = {};

  // Delete current operators
  jsPlumb.reset();
  editorContent.empty();

  deserializeWorkflowOperator('Operator', 'operator');
  deserializeWorkflowOperator('Dataset', 'dataset');

  function deserializeWorkflowOperator(elementName, childClass) {
    xmlRoot.find(elementName).each(function() {
      var xml = $(this);
      var taskId = xml.attr('task');
      var opId = xml.attr('id');
      if(opId === undefined) {
        opId = taskId
      }

      var toolbox = $("#toolbox_" + taskId);
      // Don't hide, workflow operators can be used multiple times
      //    toolbox.hide();

      var box = toolbox.children('.' + childClass).clone(false);
      box.attr('taskid', taskId);
      box.attr('id', opId)
      box.show();
      box.css({top: xml.attr('posY') + 'px', left: xml.attr('posX') + 'px', position: 'absolute'});
      box.appendTo(editorContent);

      // Make operator draggable
      jsPlumb.draggable(box);

      // Add endpoints
      sourceEndpoints[opId] = jsPlumb.addEndpoint(box, endpointSource);
      targetEndpoints[opId] = jsPlumb.addEndpoint(box, endpointTarget);
    });
  }

  connectEndpoints('Operator');
  connectEndpoints('Dataset');

  function connectEndpoints(elementName) {
    // Connect endpoints
    // Since operators are connected in both directions we only need to look at one direction, i.e. inputs.
      xmlRoot.find(elementName).each(function() {
        var xml = $(this);

        var taskId = xml.attr('id');
        // Connect inputs
        $.each(xml.attr('inputs').split(','), function() {
          if(this != "") {
            jsPlumb.connect({source: sourceEndpoints[this], target: targetEndpoints[taskId]});
          }
        });
      });
  }
}