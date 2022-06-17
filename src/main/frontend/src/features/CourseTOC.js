import Alert from 'react-bootstrap/Alert';
import CourseTopic from './CourseTopic';

function CourseTOC (props) {

  if (!props.topics || props.topics.length === 0) {
    return <Alert>This course does not have topics.</Alert>;
  }

  const courseTableOfContents = props.topics.map((topic, index) => {
    return <CourseTopic key={index} topic={topic} />;
  });

  return courseTableOfContents;

}

export default CourseTOC;
