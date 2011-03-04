/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.common.filter;

import java.util.List;
import java.util.ListIterator;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.controller.IMapSelection;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.common.filter.condition.ASelectableCondition;
import org.freeplane.features.common.map.MapChangeEvent;
import org.freeplane.features.common.map.MapModel;
import org.freeplane.features.common.map.ModeController;
import org.freeplane.features.common.map.NodeModel;

/**
 * @author Dimitry Polivaev
 */
public class Filter {
	static Filter createTransparentFilter() {
		return new Filter(null, true, false, false, false);
	}

	final private boolean appliesToVisibleNodesOnly;
	final private ASelectableCondition condition;
// // 	final private Controller controller;
	final private int options;
	final private boolean unfold;

	/**
	 * @param b 
	 */
	public Filter(final ASelectableCondition condition, final boolean areAnchestorsShown,
	              final boolean areDescendantsShown, final boolean applyToVisibleNodesOnly, final boolean unfold) {
		super();
//		this.controller = controller;
		this.condition = condition;
		int options = FilterInfo.FILTER_INITIAL_VALUE | FilterInfo.FILTER_SHOW_MATCHED;
		if (areAnchestorsShown) {
			options += FilterInfo.FILTER_SHOW_ANCESTOR;
		}
		options += FilterInfo.FILTER_SHOW_ECLIPSED;
		if (areDescendantsShown) {
			options += FilterInfo.FILTER_SHOW_DESCENDANT;
		}
		this.options = options;
		appliesToVisibleNodesOnly = condition != null && applyToVisibleNodesOnly;
		this.unfold = unfold;
	}

	void addFilterResult(final NodeModel node, final int flag) {
		node.getFilterInfo().add(flag);
	}

	protected boolean appliesToVisibleNodesOnly() {
		return appliesToVisibleNodesOnly;
	}

	static private Icon filterIcon;

	void displayFilterStatus() {
		if (filterIcon == null) {
			filterIcon = new ImageIcon(ResourceController.getResourceController().getResource("/images/filter.png"));
		}
		if (getCondition() != null) {
			Controller.getCurrentController().getViewController().addStatusInfo("filter", null, filterIcon);
		}
		else {
			Controller.getCurrentController().getViewController().removeStatus("filter");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * freeplane.controller.filter.Filter#applyFilter(freeplane.modes.MindMap)
	 */
	public void applyFilter(Object source, final MapModel map, final boolean force) {
		if (map == null) {
			return;
		}
		try {
			displayFilterStatus();
			Controller.getCurrentController().getViewController().setWaitingCursor(true);
			final Filter oldFilter = map.getFilter();
			map.setFilter(this);
			if (force || !isConditionStronger(oldFilter)) {
				final NodeModel root = map.getRootNode();
				resetFilter(root);
				if (filterChildren(root, checkNode(root), false)) {
					addFilterResult(root, FilterInfo.FILTER_SHOW_ANCESTOR);
				}
			}
			final IMapSelection selection = Controller.getCurrentController().getSelection();
			final NodeModel selected = selection.getSelected();
			final NodeModel selectedVisible = selected.getVisibleAncestorOrSelf();
			selection.keepNodePosition(selectedVisible, 0.5f, 0.5f);
			refreshMap(source, map);
			selectVisibleNode();
		}
		finally {
			Controller.getCurrentController().getViewController().setWaitingCursor(false);
		}
	}

	private boolean applyFilter(final NodeModel node,
	                            final boolean isAncestorSelected, final boolean isAncestorEclipsed,
	                            boolean isDescendantSelected) {
		final boolean conditionSatisfied = checkNode(node);
		resetFilter(node);
		if (isAncestorSelected) {
			addFilterResult(node, FilterInfo.FILTER_SHOW_DESCENDANT);
		}
		if (conditionSatisfied) {
			isDescendantSelected = true;
			addFilterResult(node, FilterInfo.FILTER_SHOW_MATCHED);
		}
		else {
			addFilterResult(node, FilterInfo.FILTER_SHOW_HIDDEN);
		}
		if (isAncestorEclipsed) {
			addFilterResult(node, FilterInfo.FILTER_SHOW_ECLIPSED);
		}
		if (filterChildren(node, conditionSatisfied || isAncestorSelected, !conditionSatisfied
		        || isAncestorEclipsed)) {
			addFilterResult(node, FilterInfo.FILTER_SHOW_ANCESTOR);
			isDescendantSelected = true;
			if (true && unfold && !isVisible(node) && node.isFolded()) {
				final ModeController modeController = Controller.getCurrentModeController();
				modeController.getMapController().setFolded(node, false);
			}
		}
		return isDescendantSelected;
	}

	/*
	 * (non-Javadoc)
	 * @see freeplane.controller.filter.Filter#areAncestorsShown()
	 */
	public boolean areAncestorsShown() {
		return 0 != (options & FilterInfo.FILTER_SHOW_ANCESTOR);
	}

	/*
	 * (non-Javadoc)
	 * @see freeplane.controller.filter.Filter#areDescendantsShown()
	 */
	public boolean areDescendantsShown() {
		return 0 != (options & FilterInfo.FILTER_SHOW_DESCENDANT);
	}

	private boolean checkNode(final NodeModel node) {
		if (condition == null) {
			return true;
		}
		if (appliesToVisibleNodesOnly && !node.isVisible()) {
			return false;
		}
		return condition.checkNode(node);
	}

	private boolean filterChildren(final NodeModel parent,
	                               final boolean isAncestorSelected, final boolean isAncestorEclipsed) {
		final ListIterator<NodeModel> iterator = Controller.getCurrentModeController().getMapController().childrenUnfolded(
		    parent);
		boolean isDescendantSelected = false;
		while (iterator.hasNext()) {
			final NodeModel node = iterator.next();
			isDescendantSelected = applyFilter(node, isAncestorSelected, isAncestorEclipsed,
			    isDescendantSelected);
		}
		return isDescendantSelected;
	}

	public Object getCondition() {
		return condition;
	}

	public boolean isConditionStronger(final Filter oldFilter) {
		return (!appliesToVisibleNodesOnly || appliesToVisibleNodesOnly == oldFilter.appliesToVisibleNodesOnly)
		        && (condition != null && condition.equals(oldFilter.getCondition()) || condition == null
		                && oldFilter.getCondition() == null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * freeplane.controller.filter.Filter#isVisible(freeplane.modes.MindMapNode)
	 */
	public boolean isVisible(final NodeModel node) {
		if (condition == null) {
			return true;
		}
		final int filterResult = node.getFilterInfo().get();
		return ((options & FilterInfo.FILTER_SHOW_ANCESTOR) != 0 || (options & FilterInfo.FILTER_SHOW_ECLIPSED) >= (filterResult & FilterInfo.FILTER_SHOW_ECLIPSED))
		        && ((options & filterResult & ~FilterInfo.FILTER_SHOW_ECLIPSED) != 0);
	}

	private void refreshMap(Object source, MapModel map) {
		Controller.getCurrentModeController().getMapController().fireMapChanged(new MapChangeEvent(source, map, Filter.class, null, this));
	}

	private void resetFilter(final NodeModel node) {
		node.getFilterInfo().reset();
	}

	private void selectVisibleNode() {
		final IMapSelection mapSelection = Controller.getCurrentController().getSelection();
		final List<NodeModel> selectedNodes = mapSelection.getSelection();
		final int lastSelectedIndex = selectedNodes.size() - 1;
		if (lastSelectedIndex == -1) {
			return;
		}
		final ListIterator<NodeModel> iterator = selectedNodes.listIterator(lastSelectedIndex);
		while (iterator.hasPrevious()) {
			final NodeModel previous = iterator.previous();
			if (!previous.isVisible()) {
				mapSelection.toggleSelected(previous);
			}
		}
		NodeModel selected = mapSelection.getSelected();
		if (!selected.isVisible()) {
			mapSelection.selectAsTheOnlyOneSelected(selected.getVisibleAncestorOrSelf());
		}
		mapSelection.setSiblingMaxLevel(mapSelection.getSelected().getNodeLevel(false));
	}

	public boolean unfoldsInvisibleNodes() {
		return unfold;
	}
}
