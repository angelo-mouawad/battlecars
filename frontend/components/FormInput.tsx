interface FormInputProps {
  label: string;
  type: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
}

export default function FormInput({ label, type, value, onChange, required }: FormInputProps) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-xs font-bold uppercase tracking-widest text-gray-400">{label}</label>
      <input
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
        required={required}
      />
    </div>
  );
}
