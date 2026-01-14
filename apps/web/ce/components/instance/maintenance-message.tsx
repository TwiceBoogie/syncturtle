export const MaintenanceMessage = () => {
  const linkMap = [
    {
      key: "mail_to",
      label: "Contact Support",
      value: "mailto:support@syncturtle.com",
    },
  ];

  return (
    <>
      <div className="flex flex-col gap-2.5">
        <h1>&#x1F6A7; Looks like Syncturtle didn&apos;t start up correctly!</h1>
        <span className="">
          Some services might have failed to start. Please check your container logs to identitfy the problem. If
          you&apos;re stuck, reach out to our support team for more help.
        </span>
      </div>
      <div className="flex">
        {linkMap.map((link) => (
          <div key={link.key}>
            <a
              href={link.value}
              target="_blank"
              rel="noopener noreferrer"
              className="text-custom-primary-100 hover: underline text-sm"
            >
              {link.label}
            </a>
          </div>
        ))}
      </div>
    </>
  );
};
