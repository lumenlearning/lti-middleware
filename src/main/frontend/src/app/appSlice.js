import { createSlice } from '@reduxjs/toolkit';

// Static data
import { STATIC_COURSE_ARRAY } from './staticData';
import { STATIC_COURSE_ARRAY_2 } from './staticData2';
import { STATIC_COURSE_ARRAY_3 } from './staticData3';

// This defines the store slice
export const appSlice = createSlice({
  // Name of the slice
  name: 'appSlice',
  // A reducer is a function that takes the state and the action and defines the next state
  // In the example defines what happens when the category selection changes.
  reducers: {
    setLoading: (state, action) => {
      state.loading = action.payload;
    },
    setCourseArray: (state, action) => {
      state.courseArray = state.filteredCourseArray = action.payload;
    },
    updateMetadata: (state, action) => {
      state.metadata = action.payload;
    },
    changeSearchInput: (state, action) => {
      const searchInputText = action.payload;
      state.searchInputText = searchInputText;
      // This filters courses depending on the text input value.
      state.filteredCourseArray = state.courseArray.slice().filter((course) => {
        return searchInputText === '' || (course.book_title && course.book_title.toLowerCase().includes(searchInputText.toLowerCase()))
      });
    },
    changeSelectedCourse: (state, action) => {
      state.selectedCourse = action.payload;
    },
  },
});

// Defines the actions that can be dispatched using dispatch and defines what happens with the state.
export const { changeSearchInput, changeSelectedCourse, setLoading, setCourseArray, updateMetadata } = appSlice.actions;
// Connects variables to the state, when you want the state values in a component use this.
export const selectSearchInputText = (state) => state.searchInputText;
export const selectSelectedCourse = (state) => state.selectedCourse;
export const selectCourseArray = (state) => state.filteredCourseArray;
export const selectMetadata = (state) => state.metadata;
export const selectIss = (state) => state.iss;
export const selectContext = (state) => state.context;
export const selectClientId = (state) => state.clientId;
export const selectDeploymentId = (state) => state.deploymentId;
export const selectIdToken = (state) => state.id_token;
export const selectLoading = (state) => state.loading;

// This is just an static function to simulate paging while doing local development.
const returnMockPage = (page) => {
  switch (page) {
    case 3:
      return STATIC_COURSE_ARRAY_3;
    case 2:
      return STATIC_COURSE_ARRAY_2;
    case 1:
    default:
      return STATIC_COURSE_ARRAY;
  }
}

// This function fetches the courses from the backend, it should be invoked when loading the application.
export const fetchCourses = (page) => (dispatch) => {
  // If no specific page is requested, request the first one.
  const requestedPage = page ? page : 1;
  // We must display an spinner when loading courses from the backend
  dispatch(setLoading(true));
  fetch(`/harmony/courses?page=${requestedPage}`)
  .then(response => {
    if (!response.ok) {
      throw new Error(`Problem fetching courses (status: ${response.status})`);
    }
    return response.json();
  }).then(json => {
    // Load the first batch of courses from the backend when the request has been resolved.
    dispatch(setCourseArray(json.records));
    // Load the pagination information for the first page.
    dispatch(updateMetadata(json.metadata));
  }).catch(reason => {
    const staticCourseArray = returnMockPage(requestedPage);
    // For local development, if the data is empty we can use static data.
    dispatch(setCourseArray(staticCourseArray.records));
    // Load the pagination information for the first page.
    dispatch(updateMetadata(staticCourseArray.metadata));
    console.error(reason);
  }).finally(() => {
    // Remove the spinner once the request has been resolved.
    dispatch(setLoading(false));
  });
};

export default appSlice.reducer;
