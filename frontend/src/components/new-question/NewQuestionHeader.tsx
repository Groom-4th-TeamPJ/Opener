import Stopwatch from '../shared/Stopwatch'

export default function NewQuestionHeader() {
  return (
    <div>
      <div className="flex justify-between">
        <h2 className="font-bold md:text-lg lg:text-xl">변형 문제</h2>
        <Stopwatch />
      </div>
    </div>
  )
}
