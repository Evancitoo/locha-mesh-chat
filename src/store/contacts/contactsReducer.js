/* eslint-disable import/prefer-default-export */
import { ActionTypes } from '../constants';

const AplicationState = {
  contacts: []
};

export const contactsReducer = (state = AplicationState, action) => {
  switch (action.type) {
    case ActionTypes.CLEAR_ALL: {
      return { ...AplicationState };
    }

    case ActionTypes.INITIAL_STATE: {
      return {
        contacts: action.payload.contacts
      };
    }
    case ActionTypes.ADD_CONTACTS: {
      return { ...state, contacts: action.payload };
    }

    case ActionTypes.DELETE_CONTACT: {
      const contacts = Object.values(state.contacts);
      const result = contacts.filter((data) => {
        const payload = action.payload.find((contact) => contact.uid === data.uid);

        return !payload;
      });

      return { contacts: result };
    }

    case ActionTypes.EDIT_CONTACT: {
      const contacts = Object.values(state.contacts);
      const result = contacts.findIndex((data) => data.uid === action.payload.uid);

      contacts[result] = action.payload;

      return { ...state, contacts: contacts.slice() };
    }

    case ActionTypes.SAVE_PHOTO: {
      const contacts = Object.values(state.contacts);
      const index = contacts.findIndex((contact) => contact.hashUID === action.id);
      contacts[index].picture = action.payload;
      contacts[index].imageHash = action.imageHash;
      return { ...state, contacts: contacts.slice() };
    }
    default: {
      return state;
    }
  }
};
