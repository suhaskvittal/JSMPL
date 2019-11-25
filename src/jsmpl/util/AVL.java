/**
 * Copyright (C) 2019 Suhas Vittal
 *
 *  This file is part of Stoch-MPL.
 *
 *  Stoch-MPL is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Stoch-MPL is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Stoch-MPL.  If not, see <https://www.gnu.org/licenses/>.
 * */

package jsmpl.util;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * Your implementation of an AVL.
 *
 * @author Suhas Vittal
 * @version 1.0
 * @userid svittal8
 * @GTID 903490690
 *
 * Collaborators: LIST ALL COLLABORATORS YOU WORKED WITH HERE
 *
 * Resources: LIST ALL NON-COURSE RESOURCES YOU CONSULTED HERE
 */
public class AVL<T extends Comparable<? super T>> {

    // Do not add new instance variables or modify existing ones.
    private AVLNode<T> root;
    private int size;

    /**
     * Constructs a new AVL.
     *
     * This constructor should initialize an empty AVL.
     *
     * Since instance variables are initialized to their default values, there
     * is no need to do anything for this constructor.
     */
    public AVL() {
        // DO NOT IMPLEMENT THIS CONSTRUCTOR!
    }

    /**
     * Constructs a new AVL.
     *
     * This constructor should initialize the AVL with the data in the
     * Collection. The data should be added in the same order it is in the
     * Collection.
     *
     * @param data the data to add to the tree
     * @throws java.lang.IllegalArgumentException if data or any element in data
     *                                            is null
     */
    public AVL(Collection<T> data) {
        if (data == null) {
            throw new IllegalArgumentException("The data is null.");
        }
        
        for (T e : data) {
            if (e == null) {
                throw new IllegalArgumentException(""
                        + "One of the data passed is null.");
            }
            this.add(e);
        }
    }
    
    /**
     * @param curr the current node
     * @return the height of the left child
     * */
    private int getLeftHeight(AVLNode<T> curr) {
        return curr.getLeft() == null 
                ? -1 : curr.getLeft().getHeight();
    }
    
    /**
     * @param curr the current node
     * @return the height of the right child
     * */
    private int getRightHeight(AVLNode<T> curr) {
        return curr.getRight() == null 
                ? -1 : curr.getRight().getHeight();
    }
    
    /**
     * @param curr the current node
     * */
    private void updateProperties(AVLNode<T> curr) {
        int leftHeight = getLeftHeight(curr);
        int rightHeight = getRightHeight(curr);
        
        curr.setHeight(
                (leftHeight > rightHeight ? leftHeight : rightHeight) + 1);
        curr.setBalanceFactor(leftHeight - rightHeight);
    }
    
    /**
     * @param curr the current node
     * @return the modified current node
     * */
    private AVLNode<T> rotateRight(AVLNode<T> curr) {
        AVLNode<T> root = curr;
        /*
         *     A
         *    / \
         *   B   C
         *  / \
         * x   y
         * 
         *     B
         *    / \
         *   x   A
         *      / \
         *     y   C
         * */
        int bf = getLeftHeight(curr) - getRightHeight(curr);
        
        if (bf > 1 && curr.getLeft().getBalanceFactor() < 0) {
            curr.setLeft(rotateLeft(curr.getLeft()));
        }
        
        AVLNode<T> lrc = curr.getLeft().getRight();
        curr = curr.getLeft();
        curr.setRight(root);
        curr.getRight().setLeft(lrc);
        
        updateProperties(curr.getRight());
        updateProperties(curr);
        
        return curr;
    }
    
    /**
     * @param curr the current node
     * @return the modified current node
     * */
    private AVLNode<T> rotateLeft(AVLNode<T> curr) {
        AVLNode<T> root = curr;
        
        int bf = getLeftHeight(curr) - getRightHeight(curr);
        
        if (bf < -1 && curr.getRight().getBalanceFactor() > 0) {
            curr.setRight(rotateRight(curr.getRight()));
        }
        
        AVLNode<T> rlc = curr.getRight().getLeft();
        curr = curr.getRight();
        curr.setLeft(root);
        curr.getLeft().setRight(rlc);
            
        updateProperties(curr.getLeft());
        updateProperties(curr);
        
        return curr;
    }
    
    /**
     * @param curr the current node
     * @return the modified current node
     * */
    private AVLNode<T> checkBalance(AVLNode<T> curr) {
        int bf = getLeftHeight(curr) - getRightHeight(curr);
        if (bf < -1) {
            curr = rotateLeft(curr);
        } else if (bf > 1) {
            curr = rotateRight(curr);
        } 
        updateProperties(curr);
        
        return curr;
    }
    
    
    /**
     * Adds the element to the tree.
     *
     * Start by adding it as a leaf like in a regular BST and then rotate the
     * tree as necessary.
     *
     * If the data is already in the tree, then nothing should be done (the
     * duplicate shouldn't get added, and size should not be incremented).
     *
     * Remember to recalculate heights and balance factors while going back
     * up the tree after adding the element, making sure to rebalance if
     * necessary.
     *
     * @param data the data to add
     * @throws java.lang.IllegalArgumentException if data is null
     */
    public void add(T data) {
        if (data == null) {
            throw new IllegalArgumentException("The data is null.");
        }
        
        if (size == 0) {
            size++;
            root = new AVLNode<T>(data);
        } else {
            root = rAddHelper(data, root);
        }
    }
    
    /**
     * @param data the data to add
     * @param curr the current node
     * @return the modified current node
     * */
    private AVLNode<T> rAddHelper(T data, AVLNode<T> curr) {
        AVLNode<T> newNode = new AVLNode<T>(data);
        if (curr == null) {
            size++;
            newNode.setBalanceFactor(0);
            newNode.setHeight(0);
            return newNode;
        } else {
            if (data.compareTo(curr.getData()) > 0) {
                curr.setRight(rAddHelper(data, curr.getRight()));
            } else if (data.compareTo(curr.getData()) < 0) {
                curr.setLeft(rAddHelper(data, curr.getLeft()));
            } 
            
            curr = checkBalance(curr);
            return curr;
        }
    }
    

    /**
     * Removes and returns the element from the tree matching the given
     * parameter.
     *
     * There are 3 cases to consider:
     * 1: The node containing the data is a leaf (no children). In this case,
     * simply remove it.
     * 2: The node containing the data has one child. In this case, simply
     * replace it with its child.
     * 3: The node containing the data has 2 children. Use the successor to
     * replace the data, NOT predecessor. As a reminder, rotations can occur
     * after removing the successor node.
     *
     * Remember to recalculate heights and balance factors while going back
     * up the tree after removing the element, making sure to rebalance if
     * necessary.
     *
     * Do not return the same data that was passed in. Return the data that
     * was stored in the tree.
     *
     * Hint: Should you use value equality or reference equality?
     *
     * @param data the data to remove
     * @return the data that was removed
     * @throws java.lang.IllegalArgumentException if the data is null
     * @throws java.util.NoSuchElementException   if the data is not found
     */
    public T remove(T data) {
        if (data == null) {
            throw new IllegalArgumentException("The data is null.");
        } else {
            AVLNode<T> removedDataWrapper = new AVLNode<T>(null);
            root = rRemoveHelper(data, root, removedDataWrapper);
            size--;
            return removedDataWrapper.getData();
        }
    }
    
    /**
     * @param data the data to remove
     * @param curr the current node
     * @param removedDataWrapper the wrapper for the removed data
     * @return the modified current node
     * */
    private AVLNode<T> rRemoveHelper(T data, AVLNode<T> curr, 
            AVLNode<T> removedDataWrapper) {
        if (curr == null) {
            throw new NoSuchElementException("The data is not in the tree.");
        } else {
            if (data.compareTo(curr.getData()) < 0) {
                curr.setLeft(
                        rRemoveHelper(data, curr.getLeft(), 
                                removedDataWrapper));
                curr = checkBalance(curr);
                return curr;
            } else if (data.compareTo(curr.getData()) > 0) {
                curr.setRight(
                        rRemoveHelper(data, curr.getRight(), 
                                removedDataWrapper));
                curr = checkBalance(curr);
                return curr;
            } else {
                AVLNode<T> left = curr.getLeft();
                AVLNode<T> right = curr.getRight();

                removedDataWrapper.setData(curr.getData());
                if (left == null && right == null) {
                    return null;
                } else if (left == null && right != null) {
                    return right;
                } else if (left != null && right == null) {
                    return left;
                } else {
                    AVLNode<T> succWrapper = new AVLNode<T>(null);
                    curr.setRight(
                            removeLeftmostChild(curr.getRight(), succWrapper));
                    curr.setData(succWrapper.getData());
                    
                    curr = checkBalance(curr);
                    return curr;
                }
            }
        }
    }
    
    /**
     * @param curr the current node
     * @param removedDataWrapper the wrapper for the removed data
     * @return the modified current node
     * */
    
    private AVLNode<T> removeLeftmostChild(AVLNode<T> curr, 
            AVLNode<T> removedDataWrapper) {
        AVLNode<T> left = curr.getLeft();
        
        if (left == null) {
            removedDataWrapper.setData(curr.getData());
            return curr.getRight();
        } else {
            curr.setLeft(
                    removeLeftmostChild(curr.getLeft(), removedDataWrapper));
            curr = checkBalance(curr);
            return curr;
        }
    }
    
    /**
     * Returns the element from the tree matching the given parameter.
     *
     * Hint: Should you use value equality or reference equality?
     *
     * Do not return the same data that was passed in. Return the data that
     * was stored in the tree.
     *
     * @param data the data to search for in the tree
     * @return the data in the tree equal to the parameter
     * @throws java.lang.IllegalArgumentException if data is null
     * @throws java.util.NoSuchElementException   if the data is not in the tree
     */
    public T get(T data) {
        if (data == null) {
            throw new IllegalArgumentException("The data is null.");
        }
        
        return rGetHelper(data, root);
    }
    
    
    /**
     * @param data the data to get
     * @param curr the current node
     * @return the data from the tree
     * */
    private T rGetHelper(T data, AVLNode<T> curr) {
        if (curr == null) {
            throw new NoSuchElementException("The data is not in the tree.");
        } else {
            if (data.compareTo(curr.getData()) < 0) {
                return rGetHelper(data, curr.getLeft());
            } else if (data.compareTo(curr.getData()) > 0) {
                return rGetHelper(data, curr.getRight());
            } else {
                return curr.getData();
            }
        }
    }

    /**
     * Returns whether or not data matching the given parameter is contained
     * within the tree.
     *
     * Hint: Should you use value equality or reference equality?
     *
     * @param data the data to search for in the tree.
     * @return true if the parameter is contained within the tree, false
     * otherwise
     * @throws java.lang.IllegalArgumentException if data is null
     */
    public boolean contains(T data) {
        try {
            get(data);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Returns the height of the root of the tree.
     *
     * @return the height of the root of the tree, -1 if the tree is empty
     */
    public int height() {
        return root == null ? -1 : root.getHeight();
    }

    /**
     * Clears the tree.
     *
     * Clears all data and resets the size.
     */
    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * In your BST homework, you worked with the concept of the predecessor, the
     * largest data that is smaller than the current data. However, you only
     * saw it in the context of the 2-child remove case.
     *
     * This method should retrieve (but not remove) the predecessor of the data
     * passed in. There are 2 cases to consider:
     * 1: The left subtree is non-empty. In this case, the predecessor is the
     * rightmost node of the left subtree.
     * 2: The left subtree is empty. In this case, the predecessor is the lowest
     * ancestor of the node containing data whose right child is also
     * an ancestor of data.
     *
     * This should NOT be used in the remove method.
     *
     * Ex:
     * Given the following AVL composed of Integers
     *                    76
     *                  /    \
     *                34      90
     *                  \    /
     *                  40  81
     * predecessor(76) should return 40
     * predecessor(81) should return 76
     *
     * @param data the data to find the predecessor of
     * @return the predecessor of data. If there is no smaller data than the
     * one given, return null.
     * @throws java.lang.IllegalArgumentException if the data is null
     * @throws java.util.NoSuchElementException   if the data is not in the tree
     */
    public T predecessor(T data) {
        if (data == null) {
            throw new IllegalArgumentException("The data is null");
        }
        
        AVLNode<T> lowestRightParent = null;
        AVLNode<T> curr = root;
        while (curr != null) {
            if (data.compareTo(curr.getData()) < 0) {
                curr = curr.getLeft();
            } else if (data.compareTo(curr.getData()) > 0) {
                lowestRightParent = curr;
                curr = curr.getRight();
            } else {
                if (curr.getLeft() == null) {
                    if (lowestRightParent == null) {
                        return null;
                    } else {
                        return lowestRightParent.getData();
                    }
                } else {
                    AVLNode<T> pred = curr.getLeft();
                    while (pred.getRight() != null) {
                        pred = pred.getRight();
                    }
                    
                    return pred.getData();
                }
            }
        }
        
        throw new NoSuchElementException("The data is not in the tree.");
    }

    /**
     * Finds and retrieves the k-smallest elements from the AVL in sorted order,
     * least to greatest.
     *
     * In most cases, this method will not need to traverse the entire tree to
     * function properly, so you should only traverse the branches of the tree
     * necessary to get the data and only do so once. Failure to do so will
     * result in an efficiency penalty.
     *
     * Ex:
     * Given the following AVL composed of Integers
     *                50
     *              /    \
     *            25      75
     *           /  \     / \
     *          12   37  70  80
     *         /  \    \      \
     *        10  15    40    85
     *           /
     *          13
     * kSmallest(0) should return the list []
     * kSmallest(5) should return the list [10, 12, 13, 15, 25].
     * kSmallest(3) should return the list [10, 12, 13].
     *
     * @param k the number of smallest elements to return
     * @return sorted list consisting of the k smallest elements
     * @throws java.lang.IllegalArgumentException if k < 0 or k > n, the number
     *                                            of data in the AVL
     */
    public List<T> kSmallest(int k) {
        if (k < 0 || k > size) {
            throw new IllegalArgumentException("k < 0 or k > " + size + ".");
        }
        
        List<T> stack = new ArrayList<T>();
        rKSmallestHelper(k, root, stack);
        return stack;
    }
    
    /**
     * @param k the number of smallest nodes to return
     * @param from the current node
     * @param stack the stack of smallest nodes
     * @return the number of nodes added to the stack
     * */
    private int rKSmallestHelper(int k, AVLNode<T> from, List<T> stack) {
        if (from == null) {
            return 0;       
        } else {
            int count = 0;
            count += rKSmallestHelper(k, from.getLeft(), stack);
            if (count >= k) {
                return count;
            }
            stack.add(from.getData());
            count++;
            if (count >= k) {
                return count;
            }
            count += rKSmallestHelper(k - count, from.getRight(), stack);
            return count;
        }
    }
    
    
    /**
     * Returns the root of the tree.
     *
     * For grading purposes only. You shouldn't need to use this method since
     * you have direct access to the variable.
     *
     * @return the root of the tree
     */
    public AVLNode<T> getRoot() {
        // DO NOT MODIFY THIS METHOD!
        return root;
    }

    /**
     * Returns the size of the tree.
     *
     * For grading purposes only. You shouldn't need to use this method since
     * you have direct access to the variable.
     *
     * @return the size of the tree
     */
    public int size() {
        // DO NOT MODIFY THIS METHOD!
        return size;
    }
}
