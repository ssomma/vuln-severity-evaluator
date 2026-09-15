(function () {
  function childList(item) {
    return Array.prototype.find.call(item.children, function (child) {
      return child.tagName === 'UL';
    });
  }

  function directLabel(item) {
    return Array.prototype.find.call(item.children, function (child) {
      return child.tagName === 'A' || child.tagName === 'P';
    });
  }

  function hasActiveDescendant(item) {
    return Boolean(item.querySelector('.active'));
  }

  function setExpanded(item, expanded) {
    item.classList.toggle('is-expanded', expanded);
    item.classList.toggle('is-collapsed', !expanded);

    var toggle = item.querySelector(':scope > .sidebar-collapse-toggle');
    if (toggle) {
      toggle.setAttribute('aria-expanded', String(expanded));
    }
  }

  function ensureToggle(item) {
    if (item.querySelector(':scope > .sidebar-collapse-toggle')) {
      return;
    }

    var label = directLabel(item);
    var title = label ? label.textContent.trim() : 'section';
    var toggle = document.createElement('button');
    toggle.className = 'sidebar-collapse-toggle';
    toggle.type = 'button';
    toggle.textContent = '›';
    toggle.setAttribute('aria-label', 'Toggle ' + title);
    toggle.addEventListener('click', function (event) {
      event.preventDefault();
      event.stopPropagation();
      setExpanded(item, item.classList.contains('is-collapsed'));
    });

    if (label && label.nextSibling) {
      item.insertBefore(toggle, label.nextSibling);
    } else {
      item.insertBefore(toggle, item.firstChild);
    }
  }

  function expandActiveBranch(root) {
    Array.prototype.forEach.call(root.querySelectorAll('.active'), function (activeElement) {
      var current = activeElement.tagName === 'LI' ? activeElement : activeElement.parentElement;
      while (current && current !== root) {
        if (current.tagName === 'LI' && childList(current)) {
          setExpanded(current, true);
        }
        current = current.parentElement;
      }
    });
  }

  function applySidebarCollapse() {
    var root = document.querySelector('.sidebar-nav > ul');
    if (!root) {
      return;
    }

    Array.prototype.forEach.call(root.querySelectorAll('li'), function (item) {
      if (!childList(item)) {
        return;
      }

      item.classList.add('has-collapsible-children');
      ensureToggle(item);
      setExpanded(item, hasActiveDescendant(item));
    });

    expandActiveBranch(root);
  }

  window.sidebarCollapsePlugin = function (hook) {
    function schedule() {
      setTimeout(applySidebarCollapse, 0);
    }

    hook.mounted(schedule);
    hook.doneEach(schedule);
  };
}());
