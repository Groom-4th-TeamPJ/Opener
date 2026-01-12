type GreetingSectionProps = {
  name: string
}

export default function GreetingSection({ name }: GreetingSectionProps) {
  return (
    <section className="flex space-x-2 mb-8 lg:mb-18">
      <span className="text-3xl lg:text-4xl">👋</span>
      <div>
        <h1 className="text-lg lg:text-xl text-text-primary font-bold leading-tight">
          {name}님 반가워요.
        </h1>
        <span className="text-sm lg:text-base">오늘도 힘차게 공부해볼까요?</span>
      </div>
    </section>
  )
}
