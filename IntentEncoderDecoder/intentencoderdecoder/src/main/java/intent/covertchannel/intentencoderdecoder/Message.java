package intent.covertchannel.intentencoderdecoder;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Message implements Iterable<String> {
    private Collection<String> fragments;
    private int lengthOfLastFragment;

    public Message(Collection<String> fragments, int lengthOfLastFragment) {
        this.fragments = fragments;
        this.lengthOfLastFragment = lengthOfLastFragment;
    }

    public Collection<String> getFragments() {
        // TODO: Return a copy instead of the actual collection for safety
        return this.fragments;
    }

    public int getLengthOfLastFragment() {
        return this.lengthOfLastFragment;
    }

    @Override
    public Iterator<String> iterator() {
        return new MessageIterator();
    }

    // TODO: Create custom Message iterator
    private class MessageIterator implements Iterator {
        private Iterator<String> iterator;

        public MessageIterator() {
            this.iterator = fragments.iterator(); // TODO: Really do use a copy
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Object next() {
            if(!iterator.hasNext()) {
                throw new NoSuchElementException();
            }

            String next = iterator.next();
            if(iterator.hasNext()) {
                return next;
            }

            return next.substring(0 , lengthOfLastFragment);
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }
}