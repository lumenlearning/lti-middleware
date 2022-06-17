import { createSlice } from '@reduxjs/toolkit';

// This defines the store slice
export const appSlice = createSlice({
  // Name of the slice
  name: 'appSlice',
  // A reducer is a function that takes the state and the action and defines the next state
  // In the example defines what happens when the category selection changes.
  reducers: {
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
export const { changeSearchInput, changeSelectedCourse } = appSlice.actions;
// Connects variables to the state, when you want the state values in a component use this.
export const selectSearchInputText = (state) => state.searchInputText;
export const selectSelectedCourse = (state) => state.selectedCourse;
export const selectCourseArray = (state) => state.filteredCourseArray;

export default appSlice.reducer;
