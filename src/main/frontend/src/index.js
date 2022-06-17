import React from 'react';
// Redux imports
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import appSlice from './app/appSlice'
// Components import
import ThemeProvider from 'react-bootstrap/ThemeProvider'
import App from './App';
// Stylesheet imports
import 'bootstrap/dist/css/bootstrap.min.css';
import 'font-awesome/css/font-awesome.min.css';
// Static data
import { STATIC_COURSE_ARRAY } from './app/staticData';
// Additional imports
import reportWebVitals from './reportWebVitals';

// Gets the element root from the DOM.
const roolElement = document.getElementById('root');
const root = createRoot(roolElement);
// Parse the LtiLaunchData JSON object from the root attribute.
const ltiLaunchData = JSON.parse(roolElement.getAttribute('lti-launch-data'));
// Use the backend attributes, it's recommended to store them in the Redux store.
// For local development, if the data is empty we can use static data.
let courseArray = STATIC_COURSE_ARRAY;
if (ltiLaunchData && ltiLaunchData.courseArray !== undefined) {
  courseArray = ltiLaunchData.courseArray;
}

// This defines the initial state of the store, the variables sent from the backend should be in the store.
const initialState = {
  courseArray: courseArray,
  searchInputText: '',
  selectedCourse: null,
  filteredCourseArray: courseArray
};

// Creates the store and preloads the initial state of the store.
const store = configureStore({
  reducer: appSlice,
  preloadedState: initialState
});

root.render(
  <React.StrictMode>
    <Provider store={store}>
      <ThemeProvider breakpoints={['xs', 'xxs','sm']}>
        <App />
      </ThemeProvider>
    </Provider>
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
